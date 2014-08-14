package models.content

import models.GeoDocumentContent
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

/** GeoDocument image content case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentImage(id: Option[Int] = None, gdocId: Int, gdocPartId: Option[Int], imageFilePath: String, width: Int, height: Int)
  extends GeoDocumentContent

/** GeoDocumentImage database table **/
class GeoDocumentImages(tag: Tag) extends Table[GeoDocumentImage](tag, "gdocument_images") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def imageFilePath = column[String]("image_file_path")
  
  def width = column[Int]("img_width")
  
  def height = column[Int]("img_height")
  
  def * = (id.?, gdocId, gdocPartId.?, imageFilePath, width, height) <> (GeoDocumentImage.tupled, GeoDocumentImage.unapply)
  
}

object GeoDocumentImages {
  
  private[models] val query = TableQuery[GeoDocumentImages]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(geoDocumentImage: GeoDocumentImage)(implicit s: Session) = query.insert(geoDocumentImage)
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentImage] =
    query.where(_.id === id).firstOption

  def deleteForGeoDocument(id: Int)(implicit s: Session) =
    query.where(_.gdocId === id).delete
  
  def findByGeoDocument(gdocId: Int)(implicit s: Session): Seq[GeoDocumentImage] =
    query.where(_.gdocId === gdocId).list
  
}
