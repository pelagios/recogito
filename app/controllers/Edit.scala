package controllers

import java.sql.Timestamp
import java.util.Date
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

/** Controller for the edit API.
  *
  * Note that the API sits behind the login wall (while read API access is 
  * available to everyone).  
  */
object Edit extends Controller with Secured {
  
  def showHistory() = DBAction { implicit session =>
    Ok(views.html.history(EditHistory.getLastN(100))) 
  }

}