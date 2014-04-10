package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.collection.mutable.HashMap
import models.stats.GeoDocumentPartStats
import scala.slick.lifted.Tag

/** Geospatial Document Part case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentPart(id: Option[Int] = None, gdocId: Int, title: String, source: Option[String] = None)
  extends GeoDocumentPartStats

/** Geospatial database table **/
class GeoDocumentParts(tag: Tag) extends Table[GeoDocumentPart](tag, "gdocument_parts") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def title = column[String]("title")
  
  def source = column[String]("source", O.Nullable)
  
  def * = (id.?, gdocId, title, source.?) <> (GeoDocumentPart.tupled, GeoDocumentPart.unapply)

}

object GeoDocumentParts {
  
  private val query = TableQuery[GeoDocumentParts]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(geoDocumentPart: GeoDocumentPart)(implicit s: Session) = query.insert(geoDocumentPart)

  /** Retrieve a GeoDocumentPart with the specified ID (= primary key) **/
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentPart] = {
    query.where(_.id === id).firstOption
  } 
    
  /** Retrieve all GeoDocumentParts for the specified GeoDocument **/
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[GeoDocumentPart] =
    query.where(_.gdocId === id).list
    
  /** Count all GeoDocumentParts for the specified GeoDocument **/
  def countForGeoDocument(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocId === id).length).first
    
  def deleteForGeoDocument(id: Int)(implicit s: Session) =
    query.where(_.gdocId === id).delete
    
    
    
  /** Retrieve a GeoDocumentPart on a specific GeoDocument that has the specified title **/
  def findByGeoDocumentAndTitle(id: Int, title: String)(implicit s: Session): Option[GeoDocumentPart] =
    query.where(_.gdocId === id).filter(_.title === title).firstOption
  
}