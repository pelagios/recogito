package controllers

import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Logger
import play.api.db.slick.Config.driver.simple._

object Edit extends Controller with Secured {
  
  def updateAnnotation(id: Int) = apiWithAuth { username => implicit requestWithSession =>
    val annotation = Annotations.findById(id)
    val user = Users.findByUsername(username)
    
    if (annotation.isDefined && user.isDefined) {
      // Update the annotation
      val body = requestWithSession.request.body
      val toponym = (body \ "toponym").as[String]
      val status = AnnotationStatus.withName((body \ "status").as[String])
      val fix = (body \ "fix").as[String]

      val updatedAnnotation = Annotation(Some(id), toponym,status, annotation.get.automatch, 
          Some(fix), annotation.get.comment, annotation.get.gdocPartId)
          
      Annotations.update(updatedAnnotation)
      
      // Create a record in the edit history
      // TODO resolve diff
      EditHistory.insert(EditEvent(None, id, user.get.id.get, Some(toponym), Some(status), Some(fix), None))
      
      Ok(Json.obj("msg" -> "ack"))
    } else {
      val msg = if (user.isEmpty) "Not logged in" else "No annotation with ID " + id
      NotFound(Json.obj("error" -> msg))
    }
  }

}