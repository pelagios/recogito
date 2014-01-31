package controllers

import controllers.io.JSONSerializer
import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json.Json
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.JsObject
import play.api.libs.json.JsArray
import play.api.mvc.AnyContent

/** Annotation CRUD controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object AnnotationController extends Controller with Secured {
  
  private val UTF8 = "UTF-8"

  /** Creates a new annotation with (corrected) toponym and offset values.
    *
    * The annotation to create is delivered as JSON in the body of the request.
    */
  def create = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val user = Users.findByUsername(username)    
    val body = requestWithSession.request.body.asJson    
    if (!body.isDefined) {    
      // No JSON body - bad request
      BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
      
    } else {
      if (body.get.isInstanceOf[JsArray]) {
        // Use recursion to insert until we get the first error message  
        def insertNext(toInsert: List[JsObject], username: String): Option[String] = {
          if (toInsert.size == 0) {
            None
          } else {
            val errorMsg = createOne(toInsert.head, username)
            if (errorMsg.isDefined)
              errorMsg
            else
              insertNext(toInsert.tail, username)
          }
        }
 
        // Insert until error message
        val errorMsg = insertNext(body.get.as[List[JsObject]], username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
            Ok(Json.parse("{ \"success\": true }"))
      } else {
        val errorMsg = createOne(body.get.as[JsObject], user.get.username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
          Ok(Json.parse("{ \"success\": true }"))
      }
    }  
  }
    
  private def createOne(json: JsObject, username: String)(implicit s: Session): Option[String] = {
    val jsonGdocId = (json\ "gdocId").as[Option[Int]] 
    val jsonGdocPartId = (json \ "gdocPartId").as[Option[Int]]  
      
    val gdocPart = jsonGdocPartId.map(id => GeoDocumentParts.findById(id)).flatten
    val gdocId_verified = if (gdocPart.isDefined) Some(gdocPart.get.gdocId) else jsonGdocId.map(id => GeoDocuments.findById(id)).flatten.map(_.id).flatten
        
    if (!gdocPart.isDefined && !(jsonGdocId.isDefined && gdocId_verified.isDefined)) {
      // Annotation specifies neither valid GDocPart nor valid GDoc - invalid annotation
      Some("{ \"success\": false, \"message\": \"Invalid GDoc or GDocPart ID\" }")
        
    } else {
      // Create new annotation
      val correctedToponym = (json \ "corrected_toponym").as[String]
      val correctedOffset = (json \ "corrected_offset").as[Int]        

      val annotation = 
        Annotation(Annotation.newUUID, gdocId_verified.get, gdocPart.map(_.id).flatten, 
                   AnnotationStatus.NOT_VERIFIED, None, None, None, 
                   Some(correctedToponym), Some(correctedOffset))
          
      if (!hasValidOffset(annotation)) {
        // Annotation is mis-aligned with source text - something is wrong
        Logger.info("Invalid offset error: " + correctedToponym + " - " + correctedOffset + " GDoc Part: " + gdocPart.get.id)
        Some("{ \"success\": false, \"message\": \"Shifted toponym alert: annotation reports invalid offset value.\" }")
          
      } else if (Annotations.getOverlappingAnnotations(annotation).size > 0) {
        // Annotation overlaps with existing ones - something is wrong
        Logger.info("Overlap error: " + correctedToponym + " - " + correctedOffset + " GDoc Part: " + gdocPart.get.id)
        Annotations.getOverlappingAnnotations(annotation).foreach(a => Logger.warn("Overlaps with " + a.uuid))
        Some("{ \"success\": false, \"message\": \"Annotation overlaps with an existing one (details were logged).\" }")
          
      } else {
        Annotations.insert(annotation)
    
        // Record edit event
        EditHistory.insert(EditEvent(None, annotation.uuid, username, new Timestamp(new Date().getTime),
          None, Some(correctedToponym), None, None, None, None))
                                                      
        None
      }
    }
        
  }
  
  /** Checks whether the annotation offset is properly aligned with the source text.
    * @param a the annotation 
    */
  private def hasValidOffset(a: Annotation)(implicit s: Session): Boolean = {
    val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
    val toponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym

    if (offset.isDefined && toponym.isDefined) {
      // Cross check against the source text, if available
      val text = GeoDocumentTexts.getTextForAnnotation(a).map(gdt => new String(gdt.text, UTF8))
      if (text.isDefined) {
        // Compare with the source text
        val referenceToponym = text.get.substring(offset.get, offset.get + toponym.get.size)
        referenceToponym.equals(toponym.get)
      } else {
        // We don't have a text for the annotation - so we'll just have to accept the offset
        true
      }
    } else {
      // Annotation has no offset and/or toponym - so isn't tied to a text, and we're cool
      true
    }
  }
  
  /** Get a specific annotation.
    * 
    * The response also includes the 'context', i.e. a snippet showing
    * the toponym with surrounding source text (if the text is available
    * in the database).
    * @param id the annotation ID
    */
  def get(uuid: UUID) = DBAction { implicit session =>
    val annotation = Annotations.findByUUID(uuid)
    if (annotation.isDefined) {          
      Ok(JSONSerializer.toJson(annotation.get, true, true))
    } else {
      NotFound
    }
  }
  
  def updateSingle(uuid: UUID) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    // Logger.info("Updating single annotation: " + uuid)
    update(Some(uuid), username)
  }
  
  def updateBatch() = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    // Logger.info("Updating annotation batch")
    update(None, username)
  }
    
  /** Updates the annotation with the specified ID.
    *  
    * @param id the annotation ID to update
    */
  private def update(uuid: Option[UUID], username: String)(implicit requestWithSession: DBSessionRequest[AnyContent]) = {
    val body = requestWithSession.request.body.asJson    
    if (!body.isDefined) {    
      // No JSON body - bad request
      BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
      
    } else {
      if (body.get.isInstanceOf[JsArray]) {
        // Use recursion to update until we get the first error message
        def updateNext(toUpdate: List[JsObject], username: String): Option[String] = {
          if (toUpdate.size == 0) {
            None
          } else {
            val errorMsg = updateOne(toUpdate.head, None, username)
            if (errorMsg.isDefined)
              errorMsg
            else
              updateNext(toUpdate.tail, username)
          }
        }
 
        // Insert until error message
        val errorMsg = updateNext(body.get.as[List[JsObject]], username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
            Ok(Json.parse("{ \"success\": true }"))
      } else {
        if (uuid.isEmpty) {
          // Single annotation in JSON body, but no UUID provided - bad request
          BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
        } else {
          val errorMsg = updateOne(body.get.as[JsObject], uuid, username)
          if (errorMsg.isDefined)
            BadRequest(Json.parse(errorMsg.get))
          else
            Ok(Json.parse("{ \"success\": true }"))
        }
      }
    } 
  }
    
  private def updateOne(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Option[String] = {    
    val annotation = if (uuid.isDefined) {
        Annotations.findByUUID(uuid.get)        
      } else {
        (json \ "id").as[Option[String]].map(uuid => Annotations.findByUUID(UUID.fromString(uuid))).flatten
      }
      
    if (!annotation.isDefined) {
      // Someone tries to update an annotation that's not in the DB
      Some("{ \"success\": false, \"message\": \"Annotation not found\" }")      
    } else { 
      val correctedStatus = (json \ "status").as[Option[String]].map(AnnotationStatus.withName(_))
      val correctedToponym = (json \ "corrected_toponym").as[Option[String]]
      val correctedOffset = (json \ "corrected_offset").as[Option[Int]]
      val correctedURI = (json \ "corrected_uri").as[Option[String]]
      val correctedTags = (json \ "tags").as[Option[String]]
      val correctedComment = (json \ "comment").as[Option[String]]
        
      val updatedStatus = correctedStatus.getOrElse(annotation.get.status)
      val updatedToponym = if (correctedToponym.isDefined) correctedToponym else annotation.get.correctedToponym
      val updatedOffset = if (correctedOffset.isDefined) correctedOffset else annotation.get.correctedOffset
      val updatedURI = if (correctedURI.isDefined) correctedURI else annotation.get.correctedGazetteerURI
      val updatedTags = if (correctedTags.isDefined) correctedTags else annotation.get.tags
      val updatedComment = if (correctedComment.isDefined) correctedComment else annotation.get.comment
   
      val toponym = if (updatedToponym.isDefined) updatedToponym else annotation.get.toponym
      val offset = if (updatedOffset.isDefined) updatedOffset else annotation.get.offset
                     
      val updated = 
        Annotation(annotation.get.uuid, annotation.get.gdocId, annotation.get.gdocPartId, 
                   updatedStatus,
                   annotation.get.toponym, annotation.get.offset, annotation.get.gazetteerURI, 
                   updatedToponym, updatedOffset, updatedURI, updatedTags, updatedComment)
          
      Annotations.update(updated)
          
      // Remove all overlapping annotations
      Annotations.getOverlappingAnnotations(updated).foreach(_delete(_))
        
      // Record edit event
      val user = Users.findByUsername(username) // The user is logged in, so we can assume the Option is defined
      EditHistory.insert(createDiffEvent(annotation.get, updated, user.get.username))
      None
    }
  }
  
  /** Deletes an annotation.
    *  
    * Note: we don't actually delete annotations, but just set their status to 'FALSE DETECTION'.
    * 
    * @param id the annotation ID 
    */
  def delete(uuid: UUID) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val annotation = Annotations.findByUUID(uuid)
    if (!annotation.isDefined) {
      // Someone tries to delete an annotation that's not in the DB
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }"))
      
    } else {
      val user = Users.findByUsername(username) // The user is logged in, so we can assume the Option is defined
      val updated = _delete(annotation.get)
        
      // Record edit event
      if (updated.isDefined)
        EditHistory.insert(createDiffEvent(annotation.get, updated.get, user.get.username))
        
      Ok(Json.parse("{ \"success\": true }"))
    } 
  }
  
  /** Deletes an annotation.
    *  
    * Note that an annotation is only really deleted if it was created manually. Annotations
    * created by the NER are not deleted, only marked as false detection.  
    * @param a the annotation
    */
  private def _delete(a: Annotation)(implicit s: Session): Option[Annotation] = {
    if (!a.toponym.isDefined) {
      Annotations.delete(a.uuid)
      None
    } else {
      val updated = Annotation(a.uuid, a.gdocId, a.gdocPartId,
                               AnnotationStatus.FALSE_DETECTION, 
                               a.toponym, a.offset,
                               a.gazetteerURI, a.correctedToponym, a.correctedOffset, a.correctedGazetteerURI,
                               a.tags, a.comment)
                                 
      Annotations.update(updated)    
      Some(updated)
    }
  }
  
  /** Private helper method that creates an update diff event by comparing original and updated annotation.
    * 
    * @param before the original annotation
    * @param after the updated annotation
    * @param userId the user who made the update
    */
  private def createDiffEvent(before: Annotation, after: Annotation, username: String)(implicit s: Session): EditEvent = {    
    val updatedStatus = if (before.status.equals(after.status)) None else Some(after.status)
    val updatedToponym = if (before.correctedToponym.equals(after.correctedToponym)) None else after.correctedToponym
    val updatedOffset = if (before.correctedOffset.equals(after.correctedOffset)) None else after.correctedOffset
    val updatedURI = if (before.correctedGazetteerURI.equals(after.correctedGazetteerURI)) None else after.correctedGazetteerURI
    val updatedTags = if (before.tags.equals(after.tags)) None else after.tags
    val updateComment = if (before.comment.equals(after.comment)) None else after.comment
    
    EditEvent(None, before.uuid, username, new Timestamp(new Date().getTime), Some(JSONSerializer.toJson(before, false, false).toString),
              updatedToponym, updatedStatus, updatedURI, updatedTags, updateComment)
  }

}