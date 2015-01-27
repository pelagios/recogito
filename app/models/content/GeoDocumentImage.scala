package models.content

import models.{ GeoDocuments, GeoDocumentParts, GeoDocumentContent }
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

object ImageType extends Enumeration {
  
  /** The annotation is not manually verified **/
  val IMAGE = Value("IMAGE")
  
  /** The annotation is not manually verified **/
  val ZOOMIFY = Value("ZOOMIFY")
  
}

/** GeoDocument image content case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentImage(id: Option[Int] = None, gdocId: Int, gdocPartId: Option[Int], imageType: ImageType.Value, width: Int, height: Int, path: String)
  extends GeoDocumentContent

/** GeoDocumentImage database table **/
class GeoDocumentImages(tag: Tag) extends Table[GeoDocumentImage](tag, "gdocument_images") {
  
  implicit val imageTypeMapper = MappedColumnType.base[ImageType.Value, String](
    { status => status.toString },
    { status => ImageType.withName(status) })

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.NotNull)
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def imageType = column[ImageType.Value]("image_type", O.NotNull)
    
  def width = column[Int]("img_width")
  
  def height = column[Int]("img_height")
  
  def path = column[String]("path")
  
  def * = (id.?, gdocId, gdocPartId.?, imageType, width, height, path) <> (GeoDocumentImage.tupled, GeoDocumentImage.unapply)
  
  /** Foreign key constraints **/
  def gdocFk = foreignKey("gdoc_fk", gdocId, TableQuery[GeoDocuments])(_.id)

  def gdocPartFk = foreignKey("gdoc_part_fk", gdocPartId, TableQuery[GeoDocumentParts])(_.id)
  
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
