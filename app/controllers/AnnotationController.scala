package controllers

import java.sql.Timestamp
import java.util.Date
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json.Json
import play.api.mvc.Controller
import play.api.Logger

/** Annotation CRUD controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object AnnotationController extends Controller with Secured {
  
  /** Creates a new annotation with (corrected) toponym and offset values **/
  def create = jsonWithAuth { username => implicit requestWithSession =>    
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val body = requestWithSession.request.body
      val gdocId = (body \ "gdocId").as[Int]
      val gdocPart = GeoDocumentParts.findById(gdocId)
      if (gdocPart.isDefined) {
        // Create new annotation
        val correctedToponym = (body \ "corrected_toponym").as[String]
        val correctedOffset = (body \ "corrected_offset").as[Int]
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
        Ok(Json.parse("{ \"success\": false, \"message\": \"Invalid GDocPart ID\" }"))
      }      
    } else {
      Forbidden(Json.parse("{ \"success\": false, \"message\": \"Not authorized\" }"))
    }
  }
  
  /** Updates the annotation with the specified ID **/
  def update(id: Int) = jsonWithAuth { username => implicit requestWithSession =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val annotation = Annotations.findById(id)
      if (annotation.isDefined) {
        // Update annotation
        val body = requestWithSession.request.body
        val correctedStatus = (body \ "status").as[Option[String]].map(AnnotationStatus.withName(_))
        val correctedToponym = (body \ "corrected_toponym").as[Option[String]]
        val correctedOffset = (body \ "corrected_offset").as[Option[Int]]
        val correctedURI = (body \ "corrected_uri").as[Option[String]]
        val correctedTags = (body \ "tags").as[Option[String]]
        val correctedComment = (body \ "comment").as[Option[String]]
        
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
        NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }")) 
      } 
    } else {
      Forbidden(Json.parse("{ \"success\": false, \"message\": \"Not authorized\" }"))
    }
  }
  
  /** 'Deletes' an annotation by setting it's status to 'FALSE DETECTION' **/
  def delete(id: Int) = dbSessionWithAuth { username => implicit requestWithSession =>
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
  
  /** Private helper method that creates an update diff event by comparing original and updated annotation **/
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