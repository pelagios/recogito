package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Annotation case class **/
case class Annotation(
    
    /** Id **/
    id: Option[Int] = None, 
    
    /** The toponym (mandatory) **/
    toponym: String, 
    
    /** Status **/
    status: AnnotationStatus.Value,
    
    /** Automatic match URI (if any) **/
    automatch: Option[String], 
    
    /** Manual correction URI (if any) **/
    fix: Option[String], 
    
    /** A comment **/
    comment: Option[String],
    
    /** Relation to the {{GeoDocumentPart}} **/
    gdocPartId: Int)
   
/** Annotation database table **/
object Annotations extends Table[Annotation]("annotations") with HasStatusColumn {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def toponym = column[String]("toponym")
  
  def status = column[AnnotationStatus.Value]("status")
  
  def automatch = column[String]("automatch")
  
  def fix = column[String]("fix")
  
  def comment = column[String]("comment")
  
  def gdocPartId = column[Int]("gdoc_part")
  
  def * = id.? ~ toponym ~ status ~ automatch.? ~ fix.? ~ comment.? ~ gdocPartId <> (Annotation.apply _, Annotation.unapply _)
  
  def findByGeoDocumentPart(id: Int)(implicit s: Session): Seq[Annotation] =
    Query(Annotations).where(_.gdocPartId === id).list
  
}
