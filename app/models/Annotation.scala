package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Annotation case class.
  *  
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
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
    gazetteerURI: Option[String] = None, 
    
    /** Toponym/correction identified by human expert **/
    correctedToponym: Option[String] = None,
    
    /** Offset of the fixed toponym **/ 
    correctedOffset: Option[Int] = None,
    
    /** Gazetteer URI identified by human expert **/
    correctedGazetteerURI: Option[String] = None,
    
    /** Tags **/
    tags: Option[String] = None,
    
    /** A comment **/
    comment: Option[String] = None,
    
    /** Source URL for the toponym **/
    source: Option[String] = None
    
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
  
  def source = column[String]("source", O.Nullable)
  
  def * = id.? ~ gdocId ~ gdocPartId.? ~ status ~ toponym.? ~ offset.? ~ gazetteerURI.? ~ correctedToponym.? ~ 
    correctedOffset.? ~ correctedGazetteerURI.? ~ tags.? ~ comment.?  ~ source.? <> (Annotation.apply _, Annotation.unapply _)
    
  private val sortByOffset = { a: Annotation =>
    val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
    if (offset.isDefined)
      (a.gdocPartId, offset.get)
    else
      (a.gdocPartId, 0)
  }
  
  def findById(id: Int)(implicit s: Session): Option[Annotation] =
    Query(Annotations).where(_.id === id).firstOption
    
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[Annotation] = {
    Query(Annotations).where(_.gdocId === id).list.sortBy(sortByOffset)      
  }
  
  def countForGeoDocument(id: Int)(implicit s: Session): Int = 
    Query(Annotations).where(_.gdocId === id).list.size
    
  def findByGeoDocumentAndStatus(id: Int, status: AnnotationStatus.Value)(implicit s: Session): Seq[Annotation] =
    Query(Annotations).where(_.gdocId === id).filter(_.status === status).list.sortBy(sortByOffset)    
  
  def countForGeoDocumentAndStatus(id: Int, status: AnnotationStatus.Value*)(implicit s: Session): Int =
    Query(Annotations).where(_.gdocId === id).filter(a => status.contains(a.status)).list.size
    
  def deleteForGeoDocument(id: Int)(implicit s: Session) =
    Query(Annotations).where(_.gdocId === id).delete
    
  def delete(id: Int)(implicit s: Session) = 
    Query(Annotations).where(_.id === id).delete
        
  def findByGeoDocumentPart(id: Int)(implicit s: Session): Seq[Annotation] =
    Query(Annotations).where(_.gdocPartId === id).list.sortBy(sortByOffset)
  
  def countForGeoDocumentPart(id: Int)(implicit s: Session): Int =
    Query(Annotations).where(_.gdocPartId === id).list.size
    
  def update(annotation: Annotation)(implicit s: Session) =
    Query(Annotations).where(_.id === annotation.id).update(annotation)
  
  def getOverlappingAnnotations(annotation: Annotation)(implicit s: Session) = {
    val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
    val offset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset else annotation.offset
    
    if (toponym.isDefined && offset.isDefined) {
      val all = if (annotation.gdocPartId.isDefined)
                  findByGeoDocumentPart(annotation.gdocPartId.get)
                else
                  findByGeoDocument(annotation.gdocId)
                  
      all.filter(a => {
        val otherToponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym
        val otherOffset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
        if (otherToponym.isDefined && otherOffset.isDefined) {
          val start = scala.math.max(otherOffset.get, offset.get)
          val end = scala.math.min(otherOffset.get + otherToponym.get.size, offset.get + toponym.get.size)
          (end - start) > 0
        } else {
          false
        }
      }).filter(_.id != annotation.id)
    } else {
      Seq.empty[Annotation]
    }
  }
  
}
