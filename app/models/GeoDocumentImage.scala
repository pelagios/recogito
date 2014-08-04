package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class GeoDocumentImage(id: Option[Int] = None, gdocId: Int, gdocPartId: Option[Int], imageFilePath: String)

class GeoDocumentImages(tag: Tag) extends Table[GeoDocumentImage](tag, "gdocument_images") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def imageFilePath = column[String]("image_file_path")
  
  def * = (id.?, gdocId, gdocPartId.?, imageFilePath) <> (GeoDocumentImage.tupled, GeoDocumentImage.unapply)
  
}

object GeoDocumentImages {
  
  private val query = TableQuery[GeoDocumentImages]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(geoDocumentImage: GeoDocumentImage)(implicit s: Session) = query.insert(geoDocumentImage)
  
  /** Retrieve a text with the specified ID (= primary key) **/
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentImage] =
    query.where(_.id === id).firstOption
    
    
  
  /** Retrieves all image records associated with a specific GeoDocument (or parts of it) **/
  def findByGeoDocument(gdocId: Int)(implicit s: Session): Seq[GeoDocumentImage] =
    query.where(_.gdocId === gdocId).list
  
}