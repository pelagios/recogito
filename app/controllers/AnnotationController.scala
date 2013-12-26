package controllers

import java.sql.Timestamp
import java.util.Date
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }

/** Annotation CRUD controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object AnnotationController extends Controller with Secured {

  /** Creates a new annotation with (corrected) toponym and offset values **/
  def create = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>    
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val body = requestWithSession.request.body.asJson
      if (body.isDefined) {
        val gdocId = (body.get \ "gdocId").as[Int]
        val gdocPart = GeoDocumentParts.findById(gdocId)
        if (gdocPart.isDefined) {
          // Create new annotation
          val correctedToponym = (body.get \ "corrected_toponym").as[String]
          val correctedOffset = (body.get \ "corrected_offset").as[Int]
        
          if (isValid(gdocPart.get.gdocId, correctedToponym, correctedOffset)) {
            val annotation = 
              Annotation(None, gdocPart.get.gdocId, gdocPart.get.id, 
                         AnnotationStatus.NOT_VERIFIED, None, None, None, 
                         Some(correctedToponym), Some(correctedOffset))
        
            val id = Annotations returning Annotations.id insert(annotation)
      
            // Record edit event
            val event = 
              EditEvent(None, id, user.get.id.get, new Timestamp(new Date().getTime), 
                        Some(correctedToponym), None, None, None, None)
                              
            Ok(Json.parse("{ \"success\": true }"))
          } else {
            BadRequest(Json.parse("{ \"success\": false, \"message\": \"Annotation did not validate\" }"))
          }
        } else {
          BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
        }
      } else {
        Ok(Json.parse("{ \"success\": false, \"message\": \"Invalid GDocPart ID\" }"))
      }      
    } else {
      Forbidden(Json.parse("{ \"success\": false, \"message\": \"Not authorized\" }"))
    }
  }
  
  /** Checks if a newly created annotation conflicts with an existing one.
   *  
    * Text annotations cannot overlap each other. If they do, this is probably a consequence
    * of identical - or contradictory - edits made concurrently by different users at the same
    * time. Overlapping annotations would also break the rendering of the text view. This 
    * sanity check method helps to ensure data integrity.
    * @param gdocPartId the ID of the annotated EGD part
    * @param toponym the annotated toponym
    * @param the annotation offset
    */
  private def isValid(gdocPartId: Int, toponym: String, offset: Int)(implicit s: Session): Boolean = {
    val intersectingAnnotations = Annotations.findByGeoDocumentPart(gdocPartId).filter(annotation => {
      val otherOffset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset else annotation.offset
      otherOffset.isDefined && (otherOffset.get >= offset) && (otherOffset.get <= offset + toponym.size)
    })    
    intersectingAnnotations.size == 0
  }
  
  /** Get a specific annotation.
    * 
    * The response also includes the 'context', i.e. a snippet showing
    * the toponym with surrounding source text (if the text is available
    * in the database).
    * @param id the annotation ID
    */
  def get(id: Int) = Action {
    // TODO implement
    Ok("")
  }
    
  /** Updates the annotation with the specified ID.
    *  
    * @param id the annotation ID to update
    */
  def update(id: Int) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val annotation = Annotations.findById(id)
      if (annotation.isDefined) {
        // Update annotation
        val body = requestWithSession.request.body.asJson
        if (body.isDefined) {
          val json = body.get
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
   
          val updated = 
            Annotation(Some(id), annotation.get.gdocId, annotation.get.gdocPartId, 
                       updatedStatus,
                       annotation.get.toponym, annotation.get.offset, annotation.get.gazetteerURI, 
                       updatedToponym, updatedOffset, updatedURI, updatedTags, updatedComment)
          
          Annotations.update(updated)
        
          // Record edit event
          EditHistory.insert(createDiffEvent(annotation.get, updated, user.get.id.get))
          Ok(Json.parse("{ \"success\": true }"))
        } else {
          BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))          
        }
      } else {
        NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }")) 
      } 
    } else {
      Forbidden(Json.parse("{ \"success\": false, \"message\": \"Not authorized\" }"))
    }
  }
  
  /** 'Deletes' an annotation by setting it's status to 'FALSE DETECTION'.
    *  
    * Note: we never actually delete an annotation from the system. We only mark
    * them as 'FALSE DETECTION' in order to maintain a full audit trail and have
    * an easy way to generate precision and recall metrics.
    * @param id the annotation ID 
    */
  def delete(id: Int) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val annotation = Annotations.findById(id)
      if (annotation.isDefined) {
        // Just update status to false detection, keep the rest as is
        val a = annotation.get
        val updated = Annotation(a.id, a.gdocId, a.gdocPartId,
                                 AnnotationStatus.FALSE_DETECTION, 
                                 a.toponym, a.offset,
                                 a.gazetteerURI, a.correctedToponym, a.correctedOffset, a.correctedGazetteerURI,
                                 a.tags, a.comment)
                                 
        Annotations.update(updated)
        
        // Record edit event
        EditHistory.insert(createDiffEvent(annotation.get, updated, user.get.id.get))
        
        Ok(Json.parse("{ \"success\": true }"))
      } else {
        NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }"))
      }
    } else {
      Forbidden(Json.parse("{ \"success\": false, \"message\": \"Not authorized\" }"))
    }
  }
  
  /** Private helper method that creates an update diff event by comparing original and updated annotation.
    * 
    * @param before the original annotation
    * @param after the updated annotation
    * @param userId the user who made the update
    */
  private def createDiffEvent(before: Annotation, after: Annotation, userId: Int): EditEvent = {    
    val updatedStatus = if (before.status.equals(after.status)) None else Some(after.status)
    val updatedToponym = if (before.correctedToponym.equals(after.correctedToponym)) None else after.correctedToponym
    val updatedOffset = if (before.correctedOffset.equals(after.correctedOffset)) None else after.correctedOffset
    val updatedURI = if (before.correctedGazetteerURI.equals(after.correctedGazetteerURI)) None else after.correctedGazetteerURI
    val updatedTags = if (before.tags.equals(after.tags)) None else after.tags
    val updateComment = if (before.comment.equals(after.comment)) None else after.comment
    
    EditEvent(None, before.id.get, userId, new Timestamp(new Date().getTime),
              updatedToponym, updatedStatus, updatedURI, updatedTags, updateComment)
  }

}