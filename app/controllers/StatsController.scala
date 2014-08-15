package controllers

import models._
import models.stats.CompletionStats
import play.api.mvc.Controller
import play.api.db.slick._

object StatsController extends Controller with Secured {
  
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
  
  /** Shows detailed stats for a specific document **/
  def showDocumentStats(docId: Int) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(docId)
    if (doc.isDefined) {        
      val id = doc.get.id.get
      val completionStats = Annotations.getCompletionStats(Seq(id)).get(id).getOrElse(CompletionStats.empty)
      val autoAnnotationStats = Annotations.getAutoAnnotationStats(id)
      val unidentifiedToponyms = Annotations.getUnidentifiableToponyms(id)
      val placeStats = Annotations.getPlaceStats(id)
      val userStats = Annotations.getContributorStats(id)
      Ok(views.html.stats.document_stats(doc.get, GeoDocumentContent.findByGeoDocument(docId), completionStats, autoAnnotationStats, userStats, unidentifiedToponyms, placeStats, currentUser.map(_.username)))
    } else {
      NotFound
    }
  }
  
  def showStats() = DBAction { implicit request =>
    val activityTimeline = StatsHistory.listRecent(50)
    val scores = EditHistory.listHighscores(20)

    // Edit events remain in the DB even if the annotations they refer to no longer exist.
    // The link Event-to-GeoDocument is defined through the annotation, so we only can
    // obtain GeoDocuments in case the annotation is still there.
    
    // Grab the events and (if the annotation still exists) the corresponding GDoc ID
    val editHistory: Seq[(EditEvent, Option[Int])] = EditHistory.getMostRecent(100)

    // Retrieve the GeoDocuments for which we have IDs
    val gdocIds = editHistory.map(_._2).filter(_.isDefined).map(_.get).distinct
    val gdocs = GeoDocuments.findByIds(gdocIds)

    // Now zip the data
    val eventsWithDocuments: Seq[(EditEvent, Option[GeoDocument])] =
      editHistory.map { case (event, gdocId) => (event, gdocId.flatMap(id => gdocs.find(_.id.get == id))) }
    
    Ok(views.html.stats.stats(activityTimeline, scores, eventsWithDocuments))
  }

}
