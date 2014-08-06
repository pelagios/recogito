package controllers

import models.Users
import play.api.mvc.Controller
import play.api.db.slick._

object StatsController extends Controller {
  
  def showUserStats(username: String) = DBAction { implicit request =>
    Users.findByUsername(username) match {
      case Some(user) => Ok(views.html.stats.user_stats(user))
      case None => NotFound
    }
  }

}