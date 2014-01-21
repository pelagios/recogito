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
  
  private val titleCache = HashMap.empty[Int, String]
  
  private val MAX_CACHE_SIZE = 10000
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def title = column[String]("title")
  
  def source = column[String]("source", O.Nullable)
  
  def * = id.? ~ gdocId ~ title ~ source.? <> (GeoDocumentPart.apply _, GeoDocumentPart.unapply _)
  
  def autoInc = id.? ~ gdocId ~ title ~ source.? <> (GeoDocumentPart.apply _, GeoDocumentPart.unapply _) returning id
  
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
  
  def getId(title: String)(implicit s: Session): Option[Int] = {
    // Somewhat clunky, but we won't be dealing with huge number of documents
    val id = titleCache.find { case(id, t) => t.equals(title) } map (_._1)
    if (id.isDefined) {
      id
    } else {
      if (titleCache.size > MAX_CACHE_SIZE)
        titleCache.clear()
        
      val part = Query(GeoDocumentParts).where(_.title === title).firstOption
      if (part.isDefined)
        titleCache.put(part.get.id.get, title)
        
      part.map(_.id).flatten
    }
  }
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentPart] =
    Query(GeoDocumentParts).where(_.id === id).firstOption
  
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[GeoDocumentPart] =
    Query(GeoDocumentParts).where(_.gdocId === id).list
    
  def countForGeoDocument(id: Int)(implicit s: Session): Int =
    Query(GeoDocumentParts).where(_.gdocId === id).list.size
  
}