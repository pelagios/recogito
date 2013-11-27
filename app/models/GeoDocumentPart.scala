package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Geospatial Document Part case class **/
case class GeoDocumentPart(id: Option[Int] = None, gdocId: Int, title: String, source: Option[String] = None)

/** Geospatial database table **/
object GeoDocumentParts extends Table[GeoDocumentPart]("gdocument_parts") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def title = column[String]("title")
  
  def source = column[String]("source", O.Nullable)
  
  def * = id.? ~ gdocId ~ title ~ source.? <> (GeoDocumentPart.apply _, GeoDocumentPart.unapply _)
  
  def findByGeoDocument(id: Int)(implicit s: Session): Seq[GeoDocumentPart] =
    Query(GeoDocumentParts).where(_.gdocId === id).list
  
}