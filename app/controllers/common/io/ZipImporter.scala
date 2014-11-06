package controllers.common.io

import collection.JavaConverters._
import global.Global
import java.io.{ BufferedOutputStream, FileOutputStream, File }
import java.util.zip.{ ZipEntry, ZipFile }
import javax.imageio.ImageIO
import models._
import models.content._
import org.apache.commons.io.IOUtils
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.io.Source
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.NameMapper
import scala.xml.XML
import scala.xml.parsing.XhtmlParser

/** Utility object to import data from a ZIP file.
  *
  * The internal structure and format of the ZIP file is compatible with that
  * produced by the ZipExporter.   
  */
object ZipImporter {
  
  private val UTF8 = "UTF-8"
    
  private val RX_HTML_ENTITY = """&[^\s]*;""".r
    
  /** Import a Zip file into Recogito 
    *
    * @param zipFile the ZIP file
    */
  def importZip(file: File)(implicit s: Session) = {
    val zipFile = new ZipFile(file)
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
        GeoDocument(None, docExtWorkID, docAuthor, docTitle, docDate, docDateComment, false,
          docLanguage, docDescription, docSource, docPrimaryTopicOf.map(_.mkString(",")),
          docOrigin, docFindspot, docAuthorLocation, None))
        
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
        val entry = zipFile.getEntry(docImage.get)
        importImage(file, docImage.get, gdocId, None)
      }
      
      // Insert parts
      if (docParts.isDefined) {
        docParts.get.zipWithIndex.foreach { case (docPart, idx) => {
          val partTitle = (docPart \ "title").as[String]
          val partSource = (docPart \ "source").asOpt[String]
          val partText = (docPart \ "text").asOpt[String]
          val partImage = (docPart \ "image").asOpt[String]
        
          // Insert the document part          
          Logger.info("... part " + partTitle)
          val gdocPartId = GeoDocumentParts.insert(GeoDocumentPart(None, gdocId, idx + 1, partTitle, partSource))
        
          if (partText.isDefined) {
            Logger.info("... text")
            importText(zipFile, partText.get, gdocId, Some(gdocPartId))
          }
          
          if (partImage.isDefined) {
            Logger.info("... image")
            importImage(file, partImage.get, gdocId, Some(gdocPartId))
          }
        }}
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
  
  def validateZip(file: File): Seq[String] = {
    val zipFile = new ZipFile(file)
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX"))
    
    def validateText(entryName: String): Option[String] = {
      val text = getEntry(zipFile, entryName)
      if (text.isDefined) {
        val plainText = text.get.getLines.mkString("\n")
        RX_HTML_ENTITY.findFirstIn(plainText).map(_ => "Text " + entryName + " contains invalid characters (HTML entities are not allowed)")
      } else {
        None
      }
    }
 
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
        docText.flatMap(validateText(_)),
        // Note: we allow upload of images without data for the time being
        // docImage.flatMap(img => if (entryExists(img, zipFile)) None else Some(name + ": referenced image file" + img + " is missing from ZIP")),
        docAnnotations.flatMap(csv => if (entryExists(csv, zipFile)) None else Some(name + ": referenced annotations file " + csv + " is missing from ZIP pacakge"))
      ) ++ docParts.flatMap(part => {
        val partText = (part \ "text").asOpt[String]
        Seq(
          partText.flatMap(txt => if (entryExists(txt, zipFile)) None else Some(name + ": referenced text file " + txt + " is missing from ZIP")),
          partText.flatMap(validateText(_))
        )
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
    * @param file the ZIP file
    * @param entryName the image file or tileset directory name within the ZIP
    * @param gdocId the ID of the GeoDocument the image is associated with
    * @param gdocPartId the ID of the GeoDocumentPart the image is associated with (if any) 
    */
  private def importImage(file: File, entryName: String, gdocId: Int, gdocPartId: Option[Int])(implicit s: Session) = {
    ZipUtil.unpack(file, Global.uploadDir, new NameMapper() {
      override def map(name: String): String = if (name.startsWith(entryName)) name else null
    })
  
    val unzippedImage = new File(Global.uploadDir, entryName)    
    if (unzippedImage.exists) {
      if (unzippedImage.isFile) {
        // Image file
        val img = ImageIO.read(unzippedImage)
        GeoDocumentImages.insert(GeoDocumentImage(None, gdocId, gdocPartId, ImageType.IMAGE, img.getWidth, img.getHeight, entryName))
      } else {
        // Tileset directory - we only support Zoomify at the moment
        val imageProperties = Source.fromFile(new File(unzippedImage, "ImageProperties.xml"))
          .getLines.mkString("\n")
          .toLowerCase // There's an ugly habit of case inconsistency in Zoomify-land, so we force XML to lowercase before parsing 
          
        val xml = XhtmlParser(Source.fromString(imageProperties))
        val w = (xml \\ "@width").toString.toInt
        val h = (xml \\ "@height").toString.toInt
        val path = if (unzippedImage.getName.endsWith("/")) unzippedImage.getName else unzippedImage.getName + "/"
        GeoDocumentImages.insert(GeoDocumentImage(None, gdocId, gdocPartId, ImageType.ZOOMIFY, w, h, path))
      }
    } else {
      // Just the metadata, without the content
      GeoDocumentImages.insert(GeoDocumentImage(None, gdocId, gdocPartId, ImageType.ZOOMIFY, 0, 0, ""))
    }
  }
  
  private def importTileset(zipFile: ZipFile, entry: ZipEntry, gdocId: Int, gdocPartId: Option[Int])(implicit s: Session) = {
    Logger.info("Hurrah!");
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
