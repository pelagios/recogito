package controllers

import models.{ Users, EditHistory }
import play.api.mvc.Controller
import play.api.db.slick._

object StatsController extends Controller {
  
  def showUserStats(username: String) = DBAction { implicit request =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val numberOfEdits = EditHistory.countForUser(username)
      val numberOfEditsPerDocument = EditHistory.countForUserPerDocument(username)
      Ok(views.html.stats.user_stats(user.get, numberOfEdits, numberOfEditsPerDocument))
    } else {
      NotFound
    } 
  }
  
  def showHighscores() = DBAction { implicit request =>
   Ok(views.html.stats.highscores(EditHistory.listHighscores(20)))
  }

}