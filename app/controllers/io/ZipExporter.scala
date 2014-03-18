package controllers.io

import models._
import java.io.{ File, FileOutputStream, PrintWriter }
import java.util.zip.ZipOutputStream
import play.api.libs.json.Json
import play.api.libs.Files.TemporaryFile
import scala.slick.session.Session
import java.util.zip.ZipEntry
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.UUID
import play.api.Logger

class ZipExporter {
  
  private val UTF8 = "UTF-8"
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")

  /** Exports one GeoDocument (with texts and annotations) to ZIP **/
  def exportGDoc(gdoc: GeoDocument)(implicit session: Session): TemporaryFile = {
    exportGDocs(Seq(gdoc))
  } 

  /** Exports a list of GeoDocuments (with text and annotations) to ZIP **/
  def exportGDocs(gdocs: Seq[GeoDocument])(implicit session: Session): TemporaryFile = {
    val zipFile = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID().toString + ".zip"))
    val zipStream = new ZipOutputStream(new FileOutputStream(zipFile.file, false))
    gdocs.foreach(exportOne(_, zipStream))
    zipStream.close()
    zipFile
  }
  
  private def exportOne(gdoc: GeoDocument, zipStream: ZipOutputStream)(implicit session: Session) = {   
    // Get GeoDocument parts & texts from the DB
    val parts = GeoDocumentParts.findByGeoDocument(gdoc.id.get)
    val texts = GeoDocumentTexts.findByGeoDocument(gdoc.id.get) 
      
    // Add JSON metadata file to ZIP
    val metadata = createMetaFile(gdoc, parts, texts)   
    addToZip(metadata.file, metadata.file.getName, zipStream)
    metadata.finalize();
    
    // Add texts to ZIP
    texts.foreach(text => {
      val textFile = new TemporaryFile(new File(TMP_DIR, "text_" + text.id.get + ".txt"))
      val textFileWriter = new PrintWriter(textFile.file)
      textFileWriter.write(new String(text.text, UTF8))
      textFileWriter.flush()
      textFileWriter.close()
      
      val textName = {
          if (text.gdocPartId.isDefined) 
            parts.find(_.id == text.gdocPartId).map(_.title)
          else
            Some(gdoc.title)
        } map (titleToFilename(_))
            
      addToZip(textFile.file, textName.map(n => titleToFilename(gdoc.title) + File.separator + n + ".txt").get, zipStream)
      textFile.finalize()
    })
    
    // Add annotations
    val annotations = Annotations.findByGeoDocument(gdoc.id.get)
    if (annotations.size > 0) {
      val annotationsFile = new TemporaryFile(new File(TMP_DIR, "annotations_" + gdoc.id.get + ".csv"))
      val annotationsFileWriter = new PrintWriter(annotationsFile.file)
      annotationsFileWriter.write(new CSVSerializer().asDBBackup(annotations))
      annotationsFileWriter.flush()
      annotationsFileWriter.close()
      addToZip(annotationsFile.file, titleToFilename(gdoc.title) + ".csv", zipStream)
      annotationsFile.finalize()
    }
  }
  
  /** Creates the document metadata JSON file for a GeoDocument **/
  private def createMetaFile(gdoc: GeoDocument, parts: Seq[GeoDocumentPart], texts: Seq[GeoDocumentText])(implicit session: Session): TemporaryFile = {
    val jsonParts = parts.map(part => {
      val text = texts.find(_.gdocPartId == part.id).map(_ => titleToFilename(gdoc.title) + File.separator + titleToFilename(part.title) + ".txt")
      Json.obj(
        "title" -> part.title,
        "source" -> part.source,
        "text" -> text
      )
    })
    
    val gdocText = texts.find(t => t.gdocId == gdoc.id.get && t.gdocPartId == None)
    val annotations =
      if (Annotations.countForGeoDocument(gdoc.id.get) > 0) Some(titleToFilename(gdoc.title) + ".csv") else None
        
    val jsonMeta = Json.obj(
      "title" -> gdoc.title,
      "author" -> gdoc.author,
      "date" -> gdoc.date,
      "date_comment" -> gdoc.dateComment,
      "description" -> gdoc.description,
      "language" -> gdoc.language,
      "source" -> gdoc.source,
      "text" -> gdocText.map(_ => titleToFilename(gdoc.title) + File.separator + titleToFilename(gdoc.title) + ".txt"),
      "annotations" -> annotations,
      "parts" -> jsonParts
    )
    
    val metaFile = new TemporaryFile(new File(TMP_DIR, titleToFilename(gdoc.title) + ".json"))
    val metaFileWriter = new PrintWriter(metaFile.file)
    metaFileWriter.write(Json.prettyPrint(jsonMeta))
    metaFileWriter.flush()
    metaFileWriter.close()    
    metaFile
  }
   
  /** Adds a file to a ZIP archive **/
  private def addToZip(file: File, filename: String, zip: ZipOutputStream) = {
    zip.putNextEntry(new ZipEntry(filename))
    val in = new BufferedInputStream(new FileInputStream(file))
    var b = in.read()
    while (b > -1) {
      zip.write(b)
      b = in.read()
    }
    in.close()
    zip.closeEntry()
  }
  
  /** Utility method to escape document titles
    *
    * We keep this separate, so we have a central location to add stuff 
    * in the future if necessary.  
    */
  def titleToFilename(title: String): String =
    title.replace(" ", "_")

}