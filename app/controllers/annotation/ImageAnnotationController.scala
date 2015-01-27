package controllers.annotation

import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.{ Json, JsArray, JsObject }
import scala.util.{ Try, Success, Failure }

trait ImageAnnotationController extends AbstractAnnotationController {
  
  protected def createOneImageAnnotation(json: JsObject, username: String)(implicit s: Session): Try[Annotation] = {
    val jsonGdocId = (json \ "gdoc_id").asOpt[Int] 
    val jsonGdocPartId = (json \ "gdoc_part_id").asOpt[Int]  

    val gdocPart = jsonGdocPartId.flatMap(id => GeoDocumentParts.findById(id))
    val gdocId_verified = if (gdocPart.isDefined) Some(gdocPart.get.gdocId) else jsonGdocId.flatMap(id => GeoDocuments.findById(id)).flatMap(_.id)
    
    if (gdocPart.isEmpty && gdocId_verified.isEmpty) {
      // Annotation specifies neither valid GDocPart nor valid GDoc - invalid annotation
      Failure(new RuntimeException("Invalid GDoc or GDocPart ID"))
        
    } else {
      // Create new annotation
      val jsonAnchor = (json \ "shapes").as[JsArray]
      
      val annotation = 
        Annotation(Annotations.newUUID, gdocId_verified, gdocPart.map(_.id).flatten, 
                   AnnotationStatus.NOT_VERIFIED, 
                   None, // toponym (automatch) 
                   None, // offset (automatch)
                   None, // anchor (automatch) 
                   None, // gazetteer URI (automatch) 
                   None, // corrected toponym
                   None, // corrected offset
                   Some(Json.stringify(jsonAnchor(0))), // corrected anchor
                   None, // corrected gazetteer URI
                   None, // tags
                   None, // comment
                   None, // source
                   None)
                   
      Annotations.insert(annotation)
    
      // Record edit event
      EditHistory.insert(EditEvent(None, annotation.uuid, username, new Timestamp(new Date().getTime),
            None, None, None, None, None, None))
      
      Success(annotation)
    }
  }
  
  protected def updateOneImageAnnotation(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Try[Annotation] = {
    val annotation = if (uuid.isDefined) {
        Annotations.findByUUID(uuid.get)        
      } else {
        (json \ "id").as[Option[String]].flatMap(uuid => Annotations.findByUUID(UUID.fromString(uuid)))
      }
    
    if (!annotation.isDefined) {
      // Someone tries to update an annotation that's not in the DB
      Failure(new RuntimeException("Annotation not found"))
      
    } else { 
      val updatedStatus = (json \ "status").asOpt[String].map(AnnotationStatus.withName(_))
      val updatedToponym = { 
          val jsonText = (json \ "text").asOpt[String]
          if (jsonText.isDefined)
            jsonText
          else
            annotation.get.correctedToponym
        }
      
      val updated = 
        Annotation(annotation.get.uuid,
                   annotation.get.gdocId,
                   annotation.get.gdocPartId,
                   updatedStatus.getOrElse(annotation.get.status), 
                   annotation.get.toponym,
                   annotation.get.offset,
                   annotation.get.anchor,
                   annotation.get.gazetteerURI,  
                   updatedToponym,
                   annotation.get.correctedOffset,
                   annotation.get.correctedAnchor,
                   annotation.get.correctedGazetteerURI,
                   annotation.get.tags,
                   annotation.get.comment,
                   annotation.get.source,
                   { if (annotation.get.seeAlso.size > 0) Some(annotation.get.seeAlso.mkString(",")) else None })
                   
      // Important: if an annotation was created manually, and someone marks it as 'false detection',
      // We delete it instead!
      if (updated.status == AnnotationStatus.FALSE_DETECTION && !updated.anchor.isDefined)
        deleteAnnotation(updated)
      else
        Annotations.update(updated)
        
      // Record edit event
      val user = Users.findByUsername(username) // The user is logged in, so we can assume the Option is defined
      EditHistory.insert(createDiffEvent(annotation.get, updated, user.get.username))
      
      Success(updated)
    }
  }

}