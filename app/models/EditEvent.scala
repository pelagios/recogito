package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Edit event case class **/
case class EditEvent(
    
    /** Annotation **/
    id: Option[Int] = None,
    
    /** Relation: ID of the annotation the edit event belongs to **/
    annotationId: Int, 
    
    /** Relation: the ID of the user who made the edit **/
    userId: Int,
    
    /** Updated toponym **/
    updatedToponym: Option[String], 
    
    /** Updated annotation status **/
    updatedStatus: Option[AnnotationStatus.Value],
        
    /** Updated place URI **/
    updatedURI: Option[String], 
    
    /** A comment **/
    updatedComment: Option[String])
    
/** Annotation database table **/
object EditHistory extends Table[EditEvent]("edit_history") with HasStatusColumn {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def annotationId = column[Int]("annotation")
  
  def userId = column[Int]("user")
  
  def updatedToponym = column[String]("updated_toponym", O.Nullable)
  
  def updatedStatus = column[AnnotationStatus.Value]("updated_status", O.Nullable)
  
  def updatedURI = column[String]("updated_uri", O.Nullable)
  
  def updatedComment = column[String]("updated_comment", O.Nullable)
  
  def * = id.? ~ annotationId ~ userId ~ updatedToponym.? ~ updatedStatus.? ~ updatedURI.? ~ updatedComment.? <> (EditEvent.apply _, EditEvent.unapply _)
  
  def findByAnnotation(id: Int)(implicit s: Session): Seq[EditEvent] =
    Query(EditHistory).where(_.annotationId === id).list
    
}