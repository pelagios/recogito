package models

import models.content._
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Generic 'interface' for different content types **/
trait GeoDocumentContent {
	
 def id: Option[Int]
 
 def gdocId: Int
 
 def gdocPartId: Option[Int]	
 
}

object GeoDocumentContent {

  def findByGeoDocument(gdocId: Int)(implicit session: Session): Seq[(GeoDocumentContent, Option[String])] = {
    val textQuery = for {
      (text, part) <- GeoDocumentTexts.query.where(_.gdocId === gdocId) leftJoin GeoDocumentParts.query on (_.gdocPartId === _.id)
    } yield (text, part.title.?)
    
    val imageQuery = for {
      (image, part) <- GeoDocumentImages.query.where(_.gdocId === gdocId) leftJoin GeoDocumentParts.query on (_.gdocPartId === _.id) 
    } yield (image, part.title.?)
    
    val texts: List[(GeoDocumentContent, Option[String])] = textQuery.list
    val images: List[(GeoDocumentContent, Option[String])] = imageQuery.list
    
    texts ++ images
  }
  
}
