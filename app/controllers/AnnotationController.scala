package controllers

import controllers.common.annotation._
import java.util.UUID
import play.api.Logger
import play.api.db.slick._
import play.api.libs.json.{ Json, JsArray, JsObject }
import play.api.mvc.RequestHeader
import models.Annotations
import controllers.common.io.JSONSerializer

object AnnotationController extends AbstractAnnotationController with TextAnnotationController with ImageAnnotationController {
  
  private def getParam(request: RequestHeader, name: String): Option[String] =
    request.queryString
      .filter(_._1.toLowerCase.equals(name))
      .headOption.flatMap(_._2.headOption)
  
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
    
  def listAnnotations() = DBAction { implicit session =>
    val gdocPartId = getParam(session.request, "gdocPart")
    val gdocId = getParam(session.request, "gdoc")
    val ctsURI = getParam(session.request, "ctsURI")
    
    if (ctsURI.isDefined) {
      // Bit of a hack - we only support this for text annotations
      Ok(forCtsURI(ctsURI.get)).withHeaders(CONTENT_TYPE -> "application/rdf+xml", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd.rdf"))
    } else if (gdocPartId.isDefined) {
      val annotations = Annotations.findByGeoDocumentPart(gdocPartId.get.toInt)
      Ok(Json.toJson(JSONSerializer.toJson(annotations)))
    } else if (gdocId.isDefined) {
      val annotations = Annotations.findByGeoDocument(gdocId.get.toInt)
      Ok(Json.toJson(JSONSerializer.toJson(annotations)))
    } else {
      BadRequest
    }
  }
    
}