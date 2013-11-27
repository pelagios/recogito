package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

case class GeoDocumentText(id: Option[Int] = None, gdocId: Option[Int], gdocPartId: Option[Int], text: Array[Byte])

object GeoDocumentTexts extends Table[GeoDocumentText]("gdocument_texts") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.Nullable)
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def text = column[Array[Byte]]("text")
  
  def * = id.? ~ gdocId.? ~ gdocPartId.? ~ text <> (GeoDocumentText.apply _, GeoDocumentText.unapply _)
  
}