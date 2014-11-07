package models

import java.sql.Timestamp
import java.util.UUID
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json.Json
import scala.slick.lifted.Tag

/** Edit event case class.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class EditEvent(
    
    /** ID **/
    id: Option[Int],
    
    /** Relation: ID of the annotation the edit event belongs to **/
    annotationId: UUID, 
    
    /** Relation: the ID of the user who made the edit **/
    username: String,
    
    /** Time and date of the edit **/
    timestamp: Timestamp,
    
    /** A serialized representation of the annotation before the edit **/
    annotationBefore: Option[String],
        
    /** Updated toponym **/
    updatedToponym: Option[String], 
    
    /** Updated annotation status **/
    updatedStatus: Option[AnnotationStatus.Value],
        
    /** Updated place URI **/
    updatedURI: Option[String], 
    
    /** Update tags **/
    updatedTags: Option[String],
    
    /** A comment **/
    updatedComment: Option[String]
    
)  {
  
  private lazy val annotationBeforeJson = annotationBefore.map(Json.parse(_)) 
  
  private def getPropertyBefore(property: String): Option[String] =
    annotationBeforeJson.flatMap(json => (json \ property).asOpt[String])
    
  lazy val toponymChange: Option[(Option[String], String)] = updatedToponym map {
    newToponym => (getPropertyBefore("toponym"), newToponym)
  } 
  
  lazy val statusChange: Option[(Option[AnnotationStatus.Value], AnnotationStatus.Value)] = updatedStatus map {
    newStatus => (getPropertyBefore("status").map(AnnotationStatus.withName(_)), newStatus)
  } 
  
  lazy val uriChange: Option[(Option[String], String)] = updatedURI map {
    newURI => {
      val place = getPropertyBefore("place")
      val placeFixed = getPropertyBefore("place_fixed")
      
      val oldURI = if (placeFixed.isDefined) placeFixed else place
      (oldURI, newURI)
    }
  }    
  
  lazy val tagChange: Option[(Option[String], String)] = updatedTags map {
    newTags => (getPropertyBefore("tags"), newTags)
  } 
  
  lazy val commentChange: Option[(Option[String], String)] = updatedComment map {
    newComment => (getPropertyBefore("comment"), newComment)
  } 
  
  lazy val annotationAfter: Option[Annotation] = {
    if (annotationBefore.isEmpty) {
      None
      
    } else {
      val before = Json.parse(annotationBefore.get)
      
      val beforeToponym = (before \ "toponym").as[Option[String]]
      val beforeStatus = (before \ "status").as[Option[String]].map(AnnotationStatus.withName(_))
      val beforePlace = (before \ "place").as[Option[String]]
      val beforePlaceFixed = (before \ "place_fixed").as[Option[String]]
      val beforeTags = (before \ "tags").as[Option[String]]
      val beforeComment = (before \ "comment").as[Option[String]]  
    
      val afterToponym = if (updatedToponym.isDefined) updatedToponym else beforeToponym
      val afterStatus = if (updatedStatus.isDefined) updatedStatus else beforeStatus
      val afterPlace = if (updatedURI.isDefined) updatedURI else if (beforePlaceFixed.isDefined) beforePlaceFixed else beforePlace
      val afterTags = if (updatedTags.isDefined) updatedTags else beforeTags
      val afterComment = if (updatedComment.isDefined) updatedComment else beforeComment
      
      Some(Annotation(annotationId, None, None, afterStatus.get, afterToponym, None))
    }
  }
  
}
    
/** Annotation database table **/
class EditHistory(tag: Tag) extends Table[EditEvent](tag, "edit_history") with HasStatusColumn {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def annotationId = column[UUID]("annotation")
  
  def username = column[String]("username")
  
  def timestamp = column[Timestamp]("timestamp")
  
  def annotationBefore = column[String]("annotation_before", O.Nullable, O.DBType("text"))
  
  def updatedToponym = column[String]("updated_toponym", O.Nullable)
  
  def updatedStatus = column[AnnotationStatus.Value]("updated_status", O.Nullable)
  
  def updatedURI = column[String]("updated_uri", O.Nullable)
  
  def updatedTags = column[String]("updated_tags", O.Nullable)
  
  def updatedComment = column[String]("updated_comment", O.Nullable)
  
  def * = (id.?, annotationId, username, timestamp, annotationBefore.?, updatedToponym.?, updatedStatus.?,
    updatedURI.?, updatedTags.?, updatedComment.?) <> (EditEvent.tupled, EditEvent.unapply)
  
}

object EditHistory {
  
  private[models] val query = TableQuery[EditHistory]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(editEvent: EditEvent)(implicit s: Session) = query.insert(editEvent)
  
  def insertAll(editEvents: Seq[EditEvent])(implicit s: Session) = query.insertAll(editEvents:_*)
  
  def findByAnnotation(uuid: UUID, limit: Int = Int.MaxValue)(implicit s: Session): Seq[EditEvent] =
    query.where(_.annotationId === uuid.bind).sortBy(_.timestamp.desc).take(limit).list
    
  def listAll()(implicit s: Session): Seq[EditEvent] =
    query.sortBy(_.timestamp.desc).list
    
  def listFromTo(from: Long, to: Long)(implicit s: Session): Seq[EditEvent] =
    query.where(_.timestamp >= new Timestamp(from)).where(_.timestamp <= new Timestamp(to)).sortBy(_.timestamp.desc).list
    
  def listFromToWithDocumentIDs(from: Long, to: Long)(implicit s: Session): Seq[(EditEvent, Option[Int])] = {
    val q = for {
      (event, annotation) <- query.where(_.timestamp >= new Timestamp(from)).where(_.timestamp <= new Timestamp(to)) leftJoin Annotations.query on (_.annotationId === _.uuid)
    } yield (event, annotation.gdocId.?)

    q.list
  }
    
  def getMostRecent(limit: Int)(implicit s: Session): Seq[(EditEvent, Option[Int])] = {
    val q = for {
      (event, annotation) <- query leftJoin Annotations.query on (_.annotationId === _.uuid)
    } yield (event, annotation.gdocId.?)
      
    q.sortBy(_._1.timestamp.desc).take(limit).list
  }
    
  def getMostRecent(limit: Int, status: AnnotationStatus.Value*)(implicit s: Session): Seq[EditEvent] =
    query.sortBy(_.timestamp.desc).filter(e => status.contains(e.updatedStatus)).take(limit).list
        
  def countSince(time: Timestamp)(implicit s: Session): Int = 
    query.where(_.timestamp > time).list.size
    
  def countForUser(username: String)(implicit s: Session): Int =
    Query(query.where(_.username === username).length).first
    
  def countForDocuments(ids: Seq[Int], since: Timestamp)(implicit s: Session): Int = {
    val q = query.where(_.timestamp > since)
                 .groupBy(_.annotationId).map(t => (t._1, t._2.length))
                 .innerJoin(Annotations.query).on(_._1 === _.uuid)
                 .filter(_._2.gdocId inSet ids)
                 .map(_._1._2)
    
    q.list.foldLeft(0)(_ + _)
  }
    
  def countForUserPerDocument(username: String)(implicit s: Session): Seq[(GeoDocument, Int)] = {
    // A list where each event is mapped to the affected annotation
    // Note: annotations can appear multiple times in this list! Since we're interested
    // in the number of events (not number of unique annotations) this is what we're intending!
    val annotationsForEvent = for {
      annotationUUID <- query.where(_.username === username).map(_.annotationId)
      annotation <- Annotations.query.where(_.uuid === annotationUUID)
    } yield annotation
    
    // Now we group by the GDocID, and map the result to a pair (GDoc, numberOfEvents) 
    val second = for {
      (gdocId, numberOfEvents) <- annotationsForEvent.groupBy(_.gdocId).map(t => (t._1, t._2.length))
      gdoc <- GeoDocuments.query.where(_.id === gdocId)
    } yield (gdoc, numberOfEvents)

    second.sortBy(_._2.desc).list
  }

  def listHighscores(limit: Int)(implicit s: Session): Seq[(User, Int)] = {
    val q = for {
      (username, numberOfEdits) <- query.groupBy(_.username).map(tuple => (tuple._1, tuple._2.length))
      user <- Users.query.where(_.username === username)
    } yield (user, numberOfEdits)
    
    q.sortBy(_._2.desc).take(limit).list
  }
      
}

object EditAnalytics {
  
  def distinctUsers(events: Seq[EditEvent]): Seq[String] =
    events.groupBy(_.username).keys.toSeq
    
  def groupByEventType(events: Seq[EditEvent]): Map[String, Seq[EditEvent]] = {
    // Types: image tag, image transcription, text selection, gazetteer assignment, other
    events.map(event => {
      if (event.annotationBefore.isEmpty && event.updatedToponym.isEmpty) {
        ("IMAGE_TAG", event)
      } else if (event.toponymChange.isDefined && event.toponymChange.get._1.isEmpty && event.annotationBefore.isDefined) {
        ("IMAGE_TRANSCRIPTION", event)
      } else if (event.toponymChange.isDefined && event.toponymChange.get._1.isEmpty && event.annotationBefore.isEmpty) {
        ("TEXT_SELECTION", event)
      } else if (event.uriChange.isDefined) {
        ("GEORESOLUTION", event)
      } else {
        ("OTHER", event)
      }
    }).groupBy(_._1).mapValues(_.map(_._2))
  }
  
}