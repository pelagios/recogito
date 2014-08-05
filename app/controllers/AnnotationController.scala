package controllers

import controllers.common.annotation._
import java.util.UUID
import play.api.Logger
import play.api.db.slick._
import play.api.libs.json.{ JsArray, JsObject }

object AnnotationController extends AbstractAnnotationController with TextAnnotationController with ImageAnnotationController {
  
  override protected def createOne(json: JsObject, username: String)(implicit s: Session): Option[String] = {
    // For the time being, we simply distinguish between text- & image-annotation based on the fact 
    // that the latter includes a 'shapes' property in the JSON
    val jsonShapes = (json\ "shapes").asOpt[JsArray]
    if (jsonShapes.isDefined)
      createOneImageAnnotation(json, username)
    else    
      createOneTextAnnotation(json, username)
  }
  
  override protected def updateOne(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Option[String] =
    updateOneTextAnnotation(json, uuid, username)
  
}