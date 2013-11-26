package controllers

import play.api.db.slick._
import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Logger

object Edit extends Controller {
  
  def updateAnnotation(id: Int) = DBAction(parse.json) { implicit requestWithSession =>
    val body = requestWithSession.request.body
    Logger.info("Storing: " + body.toString)
    Ok(Json.obj("msg" -> "ack"))    
  }

}