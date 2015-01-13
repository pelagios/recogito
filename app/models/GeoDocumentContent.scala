package models

import models.content._
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Generic interface for different types of content **/
trait GeoDocumentContent {
	
 def id: Option[Int]
 
 def gdocId: Int
 
 def gdocPartId: Option[Int]	
 
}

object GeoDocumentContent {

  /** Helper method that queries the different content tables and merges the results into a single list **/ 
  def findByGeoDocument(gdocId: Int)(implicit session: Session): Seq[(GeoDocumentContent, Option[String])] = {
    val textQuery = for {
      (text, part) <- GeoDocumentTexts.query.where(_.gdocId === gdocId) leftJoin GeoDocumentParts.query on (_.gdocPartId === _.id)
    } yield (text, part.title.?, part.sequenceNumber.?)
    
    val imageQuery = for {
      (image, part) <- GeoDocumentImages.query.where(_.gdocId === gdocId) leftJoin GeoDocumentParts.query on (_.gdocPartId === _.id) 
    } yield (image, part.title.?, part.sequenceNumber.?)
    
    val texts: List[(GeoDocumentContent, Option[String])] = textQuery.list.sortBy(_._3).map(t => (t._1, t._2))
    val images: List[(GeoDocumentContent, Option[String])] = imageQuery.list.sortBy(_._3).map(t => (t._1, t._2))
    
    texts ++ images
  }
  
  def findByGeoDocumentPart(gdocPartId: Int)(implicit session: Session): Option[GeoDocumentContent] = {
    val text = GeoDocumentTexts.query.where(_.gdocPartId === gdocPartId).firstOption
    val image = GeoDocumentImages.query.where(_.gdocPartId === gdocPartId).firstOption
    text orElse image
  }
  
}
