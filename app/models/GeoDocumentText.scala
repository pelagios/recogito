package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Geospatial Document source text case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocumentText(id: Option[Int] = None, gdocId: Int, gdocPartId: Option[Int], text: Array[Byte])

object GeoDocumentTexts extends Table[GeoDocumentText]("gdocument_texts") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc")
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def text = column[Array[Byte]]("text")
  
  def * = id.? ~ gdocId ~ gdocPartId.? ~ text <> (GeoDocumentText.apply _, GeoDocumentText.unapply _)
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocumentText] =
    Query(GeoDocumentTexts).where(_.id === id).firstOption
    
  def getTextForGeoDocument(gdocId: Int)(implicit s: Session): Option[GeoDocumentText] =
    Query(GeoDocumentTexts).where(_.gdocId === gdocId).filter(_.gdocPartId.isNull).firstOption
    
  def getTextForGeoDocumentPart(gdocPartId: Int)(implicit s: Session): Option[GeoDocumentText] =
    Query(GeoDocumentTexts).where(_.gdocPartId === gdocPartId).firstOption
    
  def getForAnnotation(annotation: Annotation)(implicit s: Session): Option[GeoDocumentText] = {    
    if (annotation.gdocPartId.isDefined) {
      Query(GeoDocumentTexts).where(_.gdocPartId === annotation.gdocPartId.get).firstOption
    } else {
      Query(GeoDocumentTexts).where(_.gdocId === annotation.gdocId).filter(_.gdocPartId.isNull).firstOption
    }
  }
  
}