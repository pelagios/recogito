package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.collection.mutable.HashMap
import scala.slick.lifted.Tag

/** Geospatial Document Part case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentPart(id: Option[Int] = None, gdocId: Int, title: String, source: Option[String] = None)

/** Geospatial database table **/
class GeoDocumentParts(tag: Tag) extends Table[GeoDocumentPart](tag, "gdocument_parts") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def title = column[String]("title")
  
  def source = column[String]("source", O.Nullable)
  
  def * = (id.?, gdocId, title, source.?) <> (GeoDocumentPart.tupled, GeoDocumentPart.unapply)
  
  /** Foreign key constraints **/
  def gdoc = foreignKey("gdoc_fk", gdocId, TableQuery[GeoDocuments])(_.id)

}

object GeoDocumentParts {
  
  private[models] val query = TableQuery[GeoDocumentParts]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(geoDocumentPart: GeoDocumentPart)(implicit s: Session) =
    query returning query.map(_.id) += geoDocumentPart

  def findById(id: Int)(implicit s: Session): Option[GeoDocumentPart] =
    query.where(_.id === id).firstOption
    
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[GeoDocumentPart] =
    query.where(_.gdocId === id).list
    
  def countForGeoDocument(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocId === id).length).first
    
  def deleteForGeoDocument(id: Int)(implicit s: Session) =
    query.where(_.gdocId === id).delete
    
  def findByGeoDocumentAndTitle(id: Int, title: String)(implicit s: Session): Option[GeoDocumentPart] =
    query.where(_.gdocId === id).filter(_.title === title).firstOption
  
}
