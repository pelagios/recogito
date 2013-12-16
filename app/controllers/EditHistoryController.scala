package controllers

import java.sql.Timestamp
import java.util.Date
import models._
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

/** Controller in charge of displaying edit history stats.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>  
  */
object EditHistoryController extends Controller with Secured {
  
  def showHistory() = DBAction { implicit session =>
    // TODO just a dummy
    Ok(views.html.history(EditHistory.getLastN(500))) 
  }

}