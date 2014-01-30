package models

import java.sql.Timestamp
import java.util.UUID
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

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
    updatedComment: Option[String])
    
/** Annotation database table **/
object EditHistory extends Table[EditEvent]("edit_history") with HasStatusColumn {

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
  
  def * = id.? ~ annotationId ~ username ~ timestamp ~ annotationBefore.? ~ updatedToponym.? ~ updatedStatus.? ~ updatedURI.? ~
    updatedTags.? ~ updatedComment.? <> (EditEvent.apply _, EditEvent.unapply _)
  
  def findByAnnotation(uuid: UUID)(implicit s: Session): Seq[EditEvent] =
    Query(EditHistory).where(_.annotationId === uuid.bind).list
    
  def getLastN(n: Int)(implicit s: Session): Seq[EditEvent] =
    Query(EditHistory).sortBy(_.timestamp.desc).take(n).list
    
  def countSince(time: Timestamp)(implicit s: Session): Int = 
    Query(EditHistory).where(_.timestamp > time).list.size
    
}