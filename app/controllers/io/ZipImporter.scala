package controllers.io

import collection.JavaConverters._
import java.io.File
import java.util.zip.ZipFile
import models._
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.io.Source

object ZipImporter {
  
  private val UTF8 = "UTF-8"
  
  def importZip(zipFile: ZipFile)(implicit s: Session) = {
    val entries = zipFile.entries.asScala.toSeq
 
    // We can have multiple JSON files in the Zip, one per document
    val jsonDescriptions = entries.filter(_.getName.endsWith(".json"))
      .map(entry => Source.fromInputStream(zipFile.getInputStream(entry)).getLines.mkString("\n"))
    
    jsonDescriptions.foreach(str => {
      val json = Json.parse(str)
      val docTitle = (json \ "title").as[String]
      val docDescription = (json \ "description").as[Option[String]]
      val docSource = (json \ "source").as[Option[String]]
      val docParts = (json \ "parts").as[List[JsObject]]
      
      // Insert the document
      val gdocId = GeoDocuments.autoInc.insert(GeoDocument(None, docTitle, docDescription, docSource))
      
      // TODO support part-less documents with text!
      
      docParts.foreach(docPart => {
        val partTitle = (docPart \ "title").as[String]
        val partSource = (docPart \ "source").as[Option[String]]
        val partText = (docPart \ "text").as[Option[String]]
        
        // Insert the document part
        val gdocPartId = GeoDocumentParts.autoInc.insert(GeoDocumentPart(None, gdocId, partTitle, partSource))
        
        if (partText.isDefined) {
          val partTextEntry = zipFile.getEntry(partText.get)
          val is = 
            if (partTextEntry == null) 
              None 
            else
              Some(zipFile.getInputStream(partTextEntry))
           
          if (is.isDefined) {
            val plainText = Source.fromInputStream(is.get, UTF8).getLines.mkString("\n")
            GeoDocumentTexts.insert(GeoDocumentText(None, gdocId, Some(gdocPartId), plainText.getBytes(UTF8)))
          }
        }
      })
    })
  }

}