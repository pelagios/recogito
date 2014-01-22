package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.collection.mutable.HashMap
import models.stats.GeoDocumentPartStats

/** Geospatial Document Part case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentPart(id: Option[Int] = None, gdocId: Int, title: String, source: Option[String] = None)
  extends GeoDocumentPartStats

/** Geospatial database table **/
object GeoDocumentParts extends Table[GeoDocumentPart]("gdocument_parts") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def title = column[String]("title")
  
  def source = column[String]("source", O.Nullable)
  
  def * = id.? ~ gdocId ~ title ~ source.? <> (GeoDocumentPart.apply _, GeoDocumentPart.unapply _)

  /** Retrieve a GeoDocumentPart with the specified ID (= primary key) **/
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentPart] = {
    Query(GeoDocumentParts).where(_.id === id).firstOption
  } 
    
  /** Retrieve all GeoDocumentParts for the specified GeoDocument **/
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[GeoDocumentPart] =
    Query(GeoDocumentParts).where(_.gdocId === id).list
    
  /** Count all GeoDocumentParts for the specified GeoDocument **/
  def countForGeoDocument(id: Int)(implicit s: Session): Int =
    Query(GeoDocumentParts).where(_.gdocId === id).list.size
    
    
  /** Retrieve a GeoDocumentPart on a specific GeoDocument that has the specified title **/
  def findByGeoDocumentAndTitle(id: Int, title: String)(implicit s: Session): Option[GeoDocumentPart] =
    Query(GeoDocumentParts).where(_.gdocId === id).filter(_.title === title).firstOption
  
  /*
  def getTitle(id: Int)(implicit s: Session): Option[String] = {
    val title = titleCache.get(id)
    if (title.isDefined) {
      title
    } else {
      // Poor mans cache - we'll just flush completely if things get too big
      if (titleCache.size > MAX_CACHE_SIZE)
        titleCache.clear()

      val title = findById(id).map(_.title)
      if (title.isDefined)
        titleCache.put(id, title.get)
        
      title
    }
  }
  */
  
}