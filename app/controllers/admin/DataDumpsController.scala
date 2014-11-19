package controllers.admin

import controllers.Secured
import models.CollectionMemberships
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.mvc.Controller
import play.api.Logger

object DataDumpsController extends Controller with Secured {
  
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.datadumps(CollectionMemberships.listCollections))
  }
  
  def annotatedPlaces(collection: String) = adminAction { username => implicit session =>
    Logger.info(collection)
    Ok("")
  }

}