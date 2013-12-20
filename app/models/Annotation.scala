package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Annotation case class **/
case class Annotation(
    
    /** Id **/
    id: Option[Int],
    
    /** Relation to the {{GeoDocument}} **/
    gdocId: Int,
    
    /** Relation to the {{GeoDocumentPart}} (if gdoc has parts) **/
    gdocPartId: Option[Int],
    
    /** Status of this annotation **/
    status: AnnotationStatus.Value,
    
    /** Toponym identified by the geoparser **/
    toponym: Option[String],
    
    /** Offset of the toponym in the text **/
    offset: Option[Int],
    
    /** Gazetteer URI identified by the georesolver **/
    gazetteerURI: Option[String], 
    
    /** Toponym/correction identified by human expert **/
    correctedToponym: Option[String] = None,
    
    /** Offset of the fixed toponym **/ 
    correctedOffset: Option[Int] = None,
    
    /** Gazetteer URI identified by human expert **/
    correctedGazetteerURI: Option[String] = None,
    
    /** Tags **/
    tags: Option[String] = None,
    
    /** A comment **/
    comment: Option[String] = None
    
)
   
/** Annotation database table **/
object Annotations extends Table[Annotation]("annotations") with HasStatusColumn {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def status = column[AnnotationStatus.Value]("status")
  
  def toponym = column[String]("toponym", O.Nullable)

  def offset = column[Int]("offset", O.Nullable)
  
  def gazetteerURI = column[String]("gazetteer_uri", O.Nullable)
  
  def correctedToponym = column[String]("toponym_corrected", O.Nullable)

  def correctedOffset = column[Int]("offset_corrected", O.Nullable)
  
  def correctedGazetteerURI = column[String]("gazetteer_uri_corrected", O.Nullable)
  
  def tags = column[String]("tags", O.Nullable)
  
  def comment = column[String]("comment", O.Nullable)
  
  def * = id.? ~ gdocId ~ gdocPartId.? ~ status ~ toponym.? ~ offset.? ~ gazetteerURI.? ~ correctedToponym.? ~ 
    correctedOffset.? ~ correctedGazetteerURI.? ~ tags.? ~ comment.?  <> (Annotation.apply _, Annotation.unapply _)
  
  def findById(id: Int)(implicit s: Session): Option[Annotation] =
    Query(Annotations).where(_.id === id).firstOption
    
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[Annotation] = {
    Query(Annotations).where(_.gdocId === id).list.sortBy(a => { 
      val offset = if (a.correctedOffset.isDefined) a.correctedOffset.get else a.offset.get
      (a.gdocPartId, offset)
    })      
  }
    
  def deleteForGeoDocument(id: Int)(implicit s: Session) =
    Query(Annotations).where(_.gdocId === id).delete
        
  def findByGeoDocumentPart(id: Int)(implicit s: Session): Seq[Annotation] = {
    Query(Annotations).where(_.gdocPartId === id).list.sortWith((a, b) => { 
      val offsetA = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
      val offsetB = if (b.correctedOffset.isDefined) b.correctedOffset else b.offset
      if (offsetA.isDefined && offsetB.isDefined)
        offsetA.get < offsetB.get
      else
        false
    })
  }
    
  def countForGeoDocumentPart(id: Int)(implicit s: Session): Int =
    Query(Annotations).where(_.gdocPartId === id).list.size
    
  def update(annotation: Annotation)(implicit s: Session) =
    Query(Annotations).where(_.id === annotation.id).update(annotation)
  
}
