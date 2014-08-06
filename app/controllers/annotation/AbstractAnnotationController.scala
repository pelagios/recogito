package controllers.annotation

import controllers.{ Secure, Secured }
import controllers.common.io.JSONSerializer
import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import play.api.Logger
import play.api.db.slick._
import play.api.mvc.{ AnyContent, Controller }
import play.api.libs.json.{ Json, JsArray, JsObject }
import scala.util.{ Try, Success, Failure }

/** Annotation CRUD controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
trait AbstractAnnotationController extends Controller with Secured {

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
            createOne(toInsert.head, username) match {
              case Success(annotation) => insertNext(toInsert.tail, username)
              case Failure(exception) => Some(exception.getMessage) 
            }
          }
        }
 
        // Insert until error message
        val errorMsg = insertNext(body.get.as[List[JsObject]], username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
          Ok(Json.parse("{ \"success\": true }"))
      } else {
        val json = body.get.as[JsObject]
        try {
          createOne(json, user.get.username) match {
            case Success(annotation) => Ok(JSONSerializer.toJson(annotation, false, false))
            case Failure(exception) => BadRequest(Json.parse("{ \"success\": false, \"message\": \"" + exception.getMessage + "\" }"))
          }
        } catch {
          case t: Throwable => {
            Logger.error("Error creating annotation: " + json)
            t.printStackTrace
            BadRequest("{ \"error\": \"" + t.getMessage + "\" }")
          }
        }
      }
    }  
  }
  
  /** Create one annotation in the DB.
    *
    * The implementation of this method depends on whether we are dealing
    * with a text or image document 
    */
  protected def createOne(json: JsObject, username: String)(implicit s: Session): Try[Annotation]
  
  /** Get a specific annotation **/
  def get(uuid: UUID) = DBAction { implicit session =>
    val annotation = Annotations.findByUUID(uuid)
    if (annotation.isDefined) {          
      Ok(JSONSerializer.toJson(annotation.get, true, true))
    } else {
      NotFound
    }
  }
  
  def updateSingle(uuid: UUID) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    update(Some(uuid), username)
  }
  
  def updateBatch() = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    update(None, username)
  }
    
  /** Updates the annotation with the specified ID **/
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
            updateOne(toUpdate.head, None, username) match {
              case Success(annotation) => updateNext(toUpdate.tail, username)
              case Failure(exception) => Some(exception.getMessage)
            }
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
          updateOne(body.get.as[JsObject], uuid, username) match {
            case Success(annotation) => Ok(JSONSerializer.toJson(annotation, false, false))
            case Failure(exception) => BadRequest(Json.parse("{ \"success\": false, \"message\": \"" + exception.getMessage + "\" }"))
          }
        }
      }
    } 
  }

  /** Update one annotation in the DB.
    *
    * The implementation of this method depends on whether we are dealing
    * with a text or image document 
    */
  protected def updateOne(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Try[Annotation]
  
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
      val updated = deleteAnnotation(annotation.get)
        
      // Record edit event
      if (updated.isDefined)
        EditHistory.insert(createDiffEvent(annotation.get, updated.get, user.get.username))
        
      Ok(Json.parse("{ \"success\": true }"))
    } 
  }
  
  /** Deletes an annotation.
    *  
    * Note that an annotation deletion is a bit of complex issue. If we're dealing with a manually created annotation, 
    * we just delete it. Period. BUT: if we deal with an annotation that was created automatically, we still want to keep
    * it in the DB for the purposes of precision/recall estimation. In this case, we therefore don't delete the
    * annotation, but just mark it as a 'false detection'. If the annotation was manually modified, we also remove those
    * manual modifications to restore the original NER state. 
    * @param a the annotation
    */
  protected def deleteAnnotation(a: Annotation)(implicit s: Session): Option[Annotation] = {
    if (a.toponym.isEmpty) {
      Annotations.delete(a.uuid)
      None
    } else {
      val updated = Annotation(a.uuid, a.gdocId, a.gdocPartId,
                               AnnotationStatus.FALSE_DETECTION, 
                               a.toponym, a.offset, a.gazetteerURI,
                               None, None, None, None, None)
                                 
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
  protected def createDiffEvent(before: Annotation, after: Annotation, username: String)(implicit s: Session): EditEvent = {    
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
