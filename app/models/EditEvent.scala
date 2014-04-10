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
  
  def annotationBefore = column[String]("annotation_before", O.Nullable)
  
  def updatedToponym = column[String]("updated_toponym", O.Nullable)
  
  def updatedStatus = column[AnnotationStatus.Value]("updated_status", O.Nullable)
  
  def updatedURI = column[String]("updated_uri", O.Nullable)
  
  def updatedTags = column[String]("updated_tags", O.Nullable)
  
  def updatedComment = column[String]("updated_comment", O.Nullable)
  
  def * = (id.?, annotationId, username, timestamp, annotationBefore.?, updatedToponym.?, updatedStatus.?,
    updatedURI.?, updatedTags.?, updatedComment.?) <> (EditEvent.tupled, EditEvent.unapply)
  
}

object EditHistory {
  
  private val query = TableQuery[EditHistory]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(editEvent: EditEvent)(implicit s: Session) = query.insert(editEvent)
  
  def insertAll(editEvents: Seq[EditEvent])(implicit s: Session) = query.insertAll(editEvents:_*)
  
  def findByAnnotation(uuid: UUID)(implicit s: Session): Seq[EditEvent] =
    query.where(_.annotationId === uuid.bind).list
    
  def getLastN(n: Int)(implicit s: Session): Seq[EditEvent] =
    query.sortBy(_.timestamp.desc).take(n).list
    
  def getLastN(n: Int, status: AnnotationStatus.Value*)(implicit s: Session): Seq[EditEvent] =
    query.sortBy(_.timestamp.desc).filter(e => status.contains(e.updatedStatus)).take(n).list
    
  def getAll()(implicit s: Session): Seq[EditEvent] =
    query.sortBy(_.timestamp.desc).list
    
  def countSince(time: Timestamp)(implicit s: Session): Int = 
    query.where(_.timestamp > time).list.size
    
}