package controllers.common.io

import collection.JavaConverters._
import global.Global
import java.io.{ BufferedOutputStream, FileOutputStream, File }
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import models._
import org.apache.commons.io.IOUtils
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.io.Source


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
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX"))
 
    // We can have multiple JSON files in the Zip, one per document
    val metafiles= entries.filter(_.getName.endsWith(".json"))
    
    Logger.info("Starting import: " + metafiles.size + " documents")
      
    metafiles.foreach(metafile => {
      Logger.info("Importing " + metafile)
            
      val json = Json.parse(Source.fromInputStream(zipFile.getInputStream(metafile), UTF8).getLines.mkString("\n"))
      
      val docExtWorkID = (json \ "ext_work_id").asOpt[String].map(_.trim)
      val docAuthor = (json \ "author").asOpt[String].map(_.trim)
      val docTitle = (json \ "title").as[String].trim
      val docDate = (json \ "date").asOpt[Int]
      val docDateComment = (json \ "date_comment").asOpt[String].map(_.trim)
      val docLanguage = (json \ "language").asOpt[String].map(_.trim)
      val docDescription = (json \ "description").asOpt[String].map(_.trim)
      val docSource = (json \ "source").asOpt[String].map(_.trim)
      val docPrimaryTopicOf = (json \ "primary_topic_of").asOpt[Seq[String]]
      val docOrigin = (json \ "origin").asOpt[String].map(_.trim)
      val docFindspot = (json \ "findspot").asOpt[String].map(_.trim)
      val docAuthorLocation = (json \ "author_location").asOpt[String].map(_.trim)
      val docCollections = (json \ "collections").asOpt[Seq[String]]
      
      val docText = (json \ "text").asOpt[String]
      val docImage = (json \ "image").asOpt[String]
      val docAnnotations = (json \ "annotations").asOpt[String]
      val docParts = (json \ "parts").asOpt[List[JsObject]]
      
      // Insert the document
      Logger.info("... document")
      val gdocId = GeoDocuments.insert(
        GeoDocument(None, docExtWorkID, docAuthor, docTitle, docDate, docDateComment,
          docLanguage, docDescription, docSource, docPrimaryTopicOf.map(_.mkString(",")),
          docOrigin, docFindspot, docAuthorLocation))
        
      // Assign to collections, if any
      if (docCollections.isDefined)
        CollectionMemberships.insertAll(docCollections.get.map(CollectionMembership(None, gdocId, _)))
      
      // Insert text (if any)
      if (docText.isDefined) {
        Logger.info("... text")
        importText(zipFile, docText.get, gdocId, None)
      }
      
      // Insert image (if any)
      if (docImage.isDefined) {
        Logger.info("... image")
        importImage(zipFile, docImage.get, gdocId, None)
      }
      
      // Insert parts
      if (docParts.isDefined) {
        docParts.get.foreach(docPart => {
          val partTitle = (docPart \ "title").as[String]
          val partSource = (docPart \ "source").asOpt[String]
          val partText = (docPart \ "text").asOpt[String]
          val partImage = (docPart \ "image").asOpt[String]
        
          // Insert the document part          
          Logger.info("... part " + partTitle)
          val gdocPartId = GeoDocumentParts.insert(GeoDocumentPart(None, gdocId, partTitle, partSource))
        
          if (partText.isDefined) {
            Logger.info("... text")
            importText(zipFile, partText.get, gdocId, Some(gdocPartId))
          }
          
          if (partImage.isDefined) {
            Logger.info("... image")
            importImage(zipFile, partImage.get, gdocId, Some(gdocPartId))
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
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX"))
 
    // We can have multiple JSON files in the Zip, one per document
    val metafiles = entries.filter(_.getName.endsWith(".json"))
    
    metafiles.flatMap(metafile => {
      val name = metafile.getName()
      val json = Json.parse(Source.fromInputStream(zipFile.getInputStream(metafile)).getLines.mkString("\n"))

      val docText = (json \ "text").asOpt[String]
      val docImage = (json \ "image").asOpt[String]
      val docAnnotations = (json \ "annotations").asOpt[String]
      val docParts = (json \ "parts").asOpt[List[JsObject]].getOrElse(List.empty[JsObject]).toSeq

      val warnings = Seq(
        docText.flatMap(txt => if (entryExists(txt, zipFile)) None else Some(name + ": referenced text file " + txt + " is missing from ZIP")),
        docImage.flatMap(img => if (entryExists(img, zipFile)) None else Some(name + ": referenced image file" + img + " is missing from ZIP")),
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
    val text = getEntry(zipFile, entryName).get    
    val plainText = text.getLines.mkString("\n")
    GeoDocumentTexts.insert(GeoDocumentText(None, gdocId, gdocPartId, plainText.getBytes(UTF8)))
  }

  /** Imports an image file.
    *
    * @param zipFile the ZIP file
    * @param entryName the name of the image file within the ZIP
    * @param gdocId the ID of the GeoDocument the image is associated with
    * @param gdocPartId the ID of the GeoDocumentPart the image is associated with (if any) 
    */
  private def importImage(zipFile: ZipFile, entryName: String, gdocId: Int, gdocPartId: Option[Int])(implicit s: Session) = {
    val entry = zipFile.getEntry(entryName)
    
    // Store the image file
    val output = new BufferedOutputStream(new FileOutputStream(new File(Global.uploadDir, entryName)))
    IOUtils.copy(zipFile.getInputStream(entry), output)
    output.flush()
    output.close()
    
    // Determine image dimensions
    val img = ImageIO.read(zipFile.getInputStream(entry))
    
    // Create DB record
    GeoDocumentImages.insert(GeoDocumentImage(None, gdocId, gdocPartId, entryName, img.getWidth, img.getHeight))
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
