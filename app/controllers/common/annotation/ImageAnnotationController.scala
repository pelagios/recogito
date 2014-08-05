package controllers.common.annotation

import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.{ Json, JsArray, JsObject }
import scala.util.{ Try, Success, Failure }

trait ImageAnnotationController {
  
  protected def createOneImageAnnotation(json: JsObject, username: String)(implicit s: Session): Try[Annotation] = {
    val jsonGdocId = (json \ "gdocId").asOpt[Int] 
    val jsonGdocPartId = (json \ "gdocPartId").asOpt[Int]  

    val gdocPart = jsonGdocPartId.flatMap(id => GeoDocumentParts.findById(id))
    val gdocId_verified = if (gdocPart.isDefined) Some(gdocPart.get.gdocId) else jsonGdocId.flatMap(id => GeoDocuments.findById(id)).flatMap(_.id)
    
    if (gdocPart.isEmpty && gdocId_verified.isEmpty) {
      // Annotation specifies neither valid GDocPart nor valid GDoc - invalid annotation
      Failure(new RuntimeException("Invalid GDoc or GDocPart ID"))
        
    } else {
      // Create new annotation
      val jsonAnchor = (json \ "shapes").as[JsArray]
      val jsonText = (json \ "text").asOpt[String]
      
      val annotation = 
        Annotation(Annotations.newUUID, gdocId_verified, gdocPart.map(_.id).flatten, 
                   AnnotationStatus.NOT_VERIFIED, 
                   None, // toponym (automatch) 
                   None, // offset (automatch)
                   None, // anchor (automatch) 
                   None, // gazetteer URI (automatch) 
                   None, // corrected toponym
                   None, // corrected offset
                   Some(Json.stringify(jsonAnchor(0))),
                   None, // corrected gazetteer URI
                   None, // tags
                   jsonText) // comment
                   
      Annotations.insert(annotation)
    
      // Record edit event
      EditHistory.insert(EditEvent(None, annotation.uuid, username, new Timestamp(new Date().getTime),
            None, None, None, None, None, None))
      
      Success(annotation)
    }
  }
  
  protected def updateOneImageAnnotation(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Option[String] = {
    // TODO implement
    None
  }

}