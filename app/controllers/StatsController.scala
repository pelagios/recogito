package controllers

import models._
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
  
  def showStats() = DBAction { implicit request =>
    val scores = EditHistory.listHighscores(20)

    // Edit events remain in the DB even if the annotations they refer to no longer exist.
    // The link Event-to-GeoDocument is defined through the annotation, so we only can
    // obtain GeoDocuments in case the annotation is still there.
    
    // Grab the events and (if the annotation still exists) the corresponding GDoc ID
    val editHistory: Seq[(EditEvent, Option[Int])] = EditHistory.getLastN(20)

    // Retrieve the GeoDocuments for which we have IDs
    val gdocIds = editHistory.map(_._2).filter(_.isDefined).map(_.get).distinct
    val gdocs = GeoDocuments.findAll(gdocIds)

    // Now zip the data
    val eventsWithDocuments: Seq[(EditEvent, Option[GeoDocument])] =
      editHistory.map { case (event, gdocId) => (event, gdocId.flatMap(id => gdocs.find(_.id.get == id))) }
    
    Ok(views.html.stats.stats(scores, eventsWithDocuments))
  }
  

  /** Shows the edit history overview page *
  def showEditHistory() = DBAction { implicit session =>
    // TODO just a dummy for now
    val history = EditHistory.getLastN(200).map(event => (event, Annotations.findByUUID(event.annotationId).flatMap(_.gdocId)))
    Ok(views.html.stats.edit_history(history)) 
  }
  * 
  */

}