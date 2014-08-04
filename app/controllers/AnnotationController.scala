package controllers

import controllers.common.annotation._
import java.util.UUID
import play.api.db.slick._
import play.api.libs.json.JsObject

object AnnotationController extends AbstractAnnotationController with TextAnnotationController {
  
  override protected def createOne(json: JsObject, username: String)(implicit s: Session): Option[String] =
    createOneTextAnnotation(json, username)
  
  override protected def updateOne(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Option[String] =
    updateOneTextAnnotation(json, uuid, username)
  
}