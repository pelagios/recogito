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
    val metafiles= entries.filter(_.getName.endsWith(".json"))
    
    Logger.info("Starting import: " + metafiles.size + " documents")
      
    metafiles.foreach(metafile => {
      Logger.info("Importing " + metafile)
      
      val json = Json.parse(Source.fromInputStream(zipFile.getInputStream(metafile)).getLines.mkString("\n"))
      val docTitle = (json \ "title").as[String]
      val docAuthor = (json \ "author").as[Option[String]]
      val docDate = (json \ "date").as[Option[Int]]
      val docDateComment = (json \ "date_comment").as[Option[String]]
      val docDescription = (json \ "description").as[Option[String]]
      val docLanguage = (json \ "language").as[Option[String]]
      val docSource = (json \ "source").as[Option[String]]
      val docText = (json \ "text").as[Option[String]]
      val docAnnotations = (json \ "annotations").as[Option[String]]
      val docParts = (json \ "parts").as[Option[List[JsObject]]]
      
      // Insert the document
      Logger.info("... document")
      val gdocId = GeoDocuments returning GeoDocuments.id insert
        GeoDocument(None, docAuthor, docTitle, docDate, docDateComment, docLanguage, docDescription, docSource)
      
      // Insert text (if any)
      if (docText.isDefined) {
        Logger.info("... text")
        importText(zipFile, docText.get, gdocId, None)
      }
      
      // Insert parts
      if (docParts.isDefined) {
        docParts.get.foreach(docPart => {
          val partTitle = (docPart \ "title").as[String]
          val partSource = (docPart \ "source").as[Option[String]]
          val partText = (docPart \ "text").as[Option[String]]
        
          // Insert the document part          
          Logger.info("... part " + partTitle)
          val gdocPartId = GeoDocumentParts returning GeoDocumentParts.id insert(GeoDocumentPart(None, gdocId, partTitle, partSource))
        
          if (partText.isDefined) {
            Logger.info("... text")
            importText(zipFile, partText.get, gdocId, Some(gdocPartId))
          }
        })
      }
      
      // Insert annotations
      if (docAnnotations.isDefined) {
        Logger.info("annotations...")
        importAnnotations(zipFile, docAnnotations.get, gdocId)
      }
    })
    
    Logger.info("Import complete")
  }
  
  private def importText(zipFile: ZipFile, entryName: String, gdocId: Int, gdocPartId: Option[Int])(implicit s: Session) = {
    val text = getEntry(zipFile, entryName)    
    if (text.isDefined) {
      val plainText = text.get.getLines.mkString("\n")
      GeoDocumentTexts.insert(GeoDocumentText(None, gdocId, gdocPartId, plainText.getBytes(UTF8)))
    }
  }
  
  private def importAnnotations(zipFile: ZipFile, entryName: String, gdocId: Int)(implicit s: Session) = {
    val csv = getEntry(zipFile, entryName)
    if (csv.isDefined) {
      val parser = new CSVParser()
      val annotations = parser.parse(csv.get, gdocId)
      Annotations.insertAll(annotations:_*)
    }
  }
  
  private def getEntry(zipFile: ZipFile, name: String): Option[Source] = {
    val entry = zipFile.getEntry(name)
    if (entry == null)
      None
    else
      Some(Source.fromInputStream(zipFile.getInputStream(entry), UTF8))
  }

}