package controllers

import global.Global
import java.sql.Timestamp
import models._
import models.stats.CompletionStats
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.Logger
import java.util.Calendar

object StatsController extends Controller with Secured {
  
  private val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
  
  def showUserStats(username: String) = DBAction { implicit request =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val numberOfEdits = EditHistory.countForUser(username)
      val numberOfEditsPerDocument = EditHistory.countForUserPerDocument(username)
      Ok(views.html.stats.userStats(user.get, numberOfEdits, numberOfEditsPerDocument))
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
      Ok(views.html.stats.documentStats(doc.get, GeoDocumentContent.findByGeoDocument(docId), completionStats, autoAnnotationStats, userStats, unidentifiedToponyms, placeStats, currentUser.map(_.username)))
    } else {
      NotFound
    }
  }

  /** Shows detailed stats for a specific toponym **/  
  def showToponymStats(toponym: String) = DBAction { implicit session =>
    // TODO grab all Gazetteer IDs for this toponym from the Annotations table
    // TODO grab all documents where the toponym appears from the Annotations table
    // TODO grab statuses
    // TODO grab all other toponyms linked to the gazetteer IDs?
    val annotations = Annotations.findByToponym(toponym)
    val byGDocIdAndPlaceURI = 
      annotations.groupBy(a => if (a.correctedGazetteerURI.isDefined) 
                                  (a.gdocId.get, a.correctedGazetteerURI)
                                else
                                  (a.gdocId.get, a.gazetteerURI))
                 .map(tuple => (tuple._1, tuple._2.size)).toSeq
             
    val documents = GeoDocuments.findByIds(byGDocIdAndPlaceURI.map(_._1._1)).map(gdoc => (gdoc.id.get, gdoc)).toMap  
    val places = byGDocIdAndPlaceURI.map(_._1._2)
      .filter(_.isDefined)
      .map(uri => Global.index.findByURI(uri.get))
      .filter(_.isDefined)
      .map(place => (place.get.uri, place.get))
      .toMap
        
    val byGDocAndPlace = byGDocIdAndPlaceURI.map(tuple =>
	  ((documents.get(tuple._1._1).get, tuple._1._2), tuple._2))
        
    Ok(views.html.stats.toponymStats(byGDocAndPlace, places))
  }
  
  /** Shows detailed stats for a specific place (= gazetteer URI) **/    
  def showPlaceStats(uri: String) = DBAction { implicit session =>
    // TODO grab all toponyms for this place
    // TODO grab all documents for this place
    // val variants: Seq[(String, Int) =  Annotations.getToponymsForPlace(uri)
    Ok("")
  }
  
  def showStats() = DBAction { implicit request =>
    /* WARNING: hacked analytics code for Heidelberg workshop
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, Calendar.OCTOBER)
    cal.set(Calendar.DAY_OF_MONTH, 31)
    cal.set(Calendar.HOUR_OF_DAY, 6)
    cal.set(Calendar.MINUTE, 0)
   
    val start = cal.getTimeInMillis
    cal.set(Calendar.MONTH, Calendar.NOVEMBER)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 9)
    val end = cal.getTimeInMillis
    
    val events = EditHistory.listFromToWithDocumentIDs(start, end)
    Logger.info("Got " + events.size + " events")
    
    // val users = EditAnalytics.distinctUsers(events.map(_._1))
    // Logger.info(users.size + " users")
    // users.foreach(u => Logger.info(u))
    
    // val byType = EditAnalytics.groupByEventType(events.map(_._1))
    // byType.foreach { case (typ, edits) =>
    //  Logger.info(typ + " - " + edits.size) }
    
    val docIds = events.flatMap(_._2).distinct
    Logger.info(docIds.size + " documents")
    
    val docsWithContent = GeoDocuments.findByIdsWithContent(docIds)
    val textDocs = docsWithContent.filter(_._2.size > 0)
    val imageDocs = docsWithContent.filter(_._3.size > 0)
    Logger.info(textDocs.size + " text documents")
    Logger.info(imageDocs.size + " image documents")
    
    val textDocsByLanguage = textDocs.map(_._1).groupBy(_.language)
    textDocsByLanguage.foreach { case (language, docs) => {
      Logger.info(language + " -> " + docs.size)
    }}
    */
    
    // Get activity timeline from DB and append today's live stats
    val activityTimeline = {
      val history = GlobalStatsHistory.listRecent(70)
      
      // Time of last history snapshot, or 24hrs if no history yet 
      val liveIntervalStart = history.reverse.headOption.map(_.timestamp).getOrElse(new Timestamp(System.currentTimeMillis - DAY_IN_MILLIS))
      val liveIntervalEnd = new Timestamp(System.currentTimeMillis + DAY_IN_MILLIS) 
      
      val liveActivity = EditHistory.countSince(liveIntervalStart)
      
      // TODO compute other live stats
      
      history :+ StatsHistoryRecord(None, liveIntervalEnd, 0, 0, 0, liveActivity) 
    }
    
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
    
    Ok(views.html.stats.globalStats(activityTimeline, scores, eventsWithDocuments))
  }

}
