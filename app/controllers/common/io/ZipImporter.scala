package controllers.common.io

import collection.JavaConverters._
import java.io.File
import java.util.zip.ZipFile
import models._
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.io.Source
import play.api.libs.json.JsValue

/** Utility object to import data from a ZIP file.
  *
  * The internal structure and format of the ZIP file is compatible with that
  * produced by the ZipExporter.   
  */
object ZipImporter {
  
  private val UTF8 = "UTF-8"
  
  /** Import a Zip file into Recogito 
    *
    * @param zipFile the ZIP file
    */
  def importZip(zipFile: ZipFile)(implicit s: Session) = {
    val entries = zipFile.entries.asScala.toSeq
 
    // We can have multiple JSON files in the Zip, one per document
    val metafiles= entries.filter(_.getName.endsWith(".json"))
    
    Logger.info("Starting import: " + metafiles.size + " documents")
      
    metafiles.foreach(metafile => {
      Logger.info("Importing " + metafile)
            
      val json = Json.parse(Source.fromInputStream(zipFile.getInputStream(metafile)).getLines.mkString("\n"))
      
      val docExtWorkID = (json \ "external_work_id").as[Option[String]]
      val docAuthor = (json \ "author").as[Option[String]]
      val docTitle = (json \ "title").as[String]
      val docDate = (json \ "date").as[Option[Int]]
      val docDateComment = (json \ "date_comment").as[Option[String]]
      val docLanguage = (json \ "language").as[Option[String]]
      val docDescription = (json \ "description").as[Option[String]]
      val docSource = (json \ "source").as[Option[String]]
      val docCollections = (json \ "source").as[Option[String]]
      
      val docText = (json \ "text").as[Option[String]]
      val docAnnotations = (json \ "annotations").as[Option[String]]
      val docParts = (json \ "parts").as[Option[List[JsObject]]]
      
      // Insert the document
      Logger.info("... document")
      val gdocId = GeoDocuments.insert(
        GeoDocument(None, docExtWorkID, docAuthor, docTitle, docDate, docDateComment, docLanguage, docDescription, docSource))
      
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
          val gdocPartId = GeoDocumentParts.insert(GeoDocumentPart(None, gdocId, partTitle, partSource))
        
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
    metafiles.size
  }
  
  def validateZip(zipFile: ZipFile): Seq[String] = {
    val entries = zipFile.entries.asScala.toSeq
 
    // We can have multiple JSON files in the Zip, one per document
    val metafiles = entries.filter(_.getName.endsWith(".json"))
    
    metafiles.flatMap(metafile => {
      val name = metafile.getName()
      val json = Json.parse(Source.fromInputStream(zipFile.getInputStream(metafile)).getLines.mkString("\n"))

      val docText = (json \ "text").as[Option[String]]
      val docAnnotations = (json \ "annotations").as[Option[String]]
      val docParts = (json \ "parts").as[Option[List[JsObject]]].getOrElse(List.empty[JsObject]).toSeq

      val warnings = Seq(
        docText.flatMap(txt => if (entryExists(txt, zipFile)) None else Some(name + ": referenced text file " + txt + " is missing from ZIP")),
        docAnnotations.flatMap(csv => if (entryExists(csv, zipFile)) None else Some(name + ": referenced annotations file " + csv + " is missing from ZIP pacakge"))
      ) ++ docParts.map(part => {
        val partText = (part \ "text").as[Option[String]]
        partText.flatMap(txt => if (entryExists(txt, zipFile)) None else Some(name + ": referenced text file " + txt + " is missing from ZIP"))
      })
    
      warnings.filter(_.isDefined).map(_.get)
    })
  }
  
  /** Imports UTF-8 plaintext.
    *
    * @param zipFile the ZIP file
    * @param entryName the name of the text file within the ZIP
    * @param gdocId the ID of the GeoDocument the text is associated with
    * @param gdocPartId the ID of the GeoDocumentPart the text is associated with (if any) 
    */
  private def importText(zipFile: ZipFile, entryName: String, gdocId: Int, gdocPartId: Option[Int])(implicit s: Session) = {
    val text = getEntry(zipFile, entryName)    
    if (text.isDefined) {
      val plainText = text.get.getLines.mkString("\n")
      GeoDocumentTexts.insert(GeoDocumentText(None, gdocId, gdocPartId, plainText.getBytes(UTF8)))
    }
  }
  
  /** Imports annotations from a CSV.
    *
    * @param zipFile the ZIP file
    * @param entryName the name of the CSV file within the ZIP
    * @param gdocId the ID of the GeoDocument the annotations are associated with
    */  
  private def importAnnotations(zipFile: ZipFile, entryName: String, gdocId: Int)(implicit s: Session) = {
    // Some rules for checking annotation sanity before import 
    def isValid(annotation: Annotation): Boolean = annotation match {
      case a if (a.toponym.isDefined && a.toponym.get.size > 254) => false
      case a if (a.correctedToponym.isDefined && a.correctedToponym.get.size > 254) => false
      case _ => true
    }
    
    val csv = getEntry(zipFile, entryName)
    if (csv.isDefined) {
      val parser = new CSVParser()
      val all = parser.parseAnnotations(csv.get, gdocId)
      
      // Apply a few checks and discard those
      val safe = all.filter(isValid(_))
      if (safe.size > 0)
        // Log warnings in case we have invalid annotations
        all.diff(safe).foreach(a => Logger.warn("Discarding annotation: " + a.toString))
      
      Annotations.insertAll(safe)
    }
  }
  
  private def entryExists(name: String, zipFile: ZipFile): Boolean =
    zipFile.getEntry(name) != null
  
  /** Helper method to get an entry from a ZIP file.
    *
    * @param zipFile the ZIP file
    * @param name the entry's name within the ZIP 
    */
  private def getEntry(zipFile: ZipFile, name: String): Option[Source] = {
    val entry = zipFile.getEntry(name)
    if (entry == null)
      None
    else
      Some(Source.fromInputStream(zipFile.getInputStream(entry), UTF8))
  }

}