package controllers

import controllers.io.{ CSVParser, CSVSerializer, JSONSerializer, ZipImporter }
import java.util.zip.ZipFile
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.Logger

/** Administration features.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AdminController extends Controller with Secured {
  
  /** Admin index page **/
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin())
  }
  
  /** Upload document (metadata + text) as a ZIP **/
  def uploadDocuments = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("zip").map(filePart => ZipImporter.importZip(new ZipFile(filePart.ref.file)))
      Redirect(routes.AdminController.index)
    } else {
      BadRequest
    }
  }
  
  /** Generates a CSV backup for the specified document or document part.
    *
    * Either a document ID or a document part ID must be provided. If neither is
    * provided, the method will return HTTP 404. If both are provided, a backup will
    * be created for the specified document. The part ID will be ignored.
    * @param doc the document ID (optional)
    * @param part the document part ID (optional)
    */
  def backupAnnotations(doc: Option[Int], part: Option[Int]) = adminAction { username => implicit session =>
    if (doc.isDefined)
      backupAnnotations_forDoc(doc.get)
    else if (part.isDefined)
      backupAnnotations_forPart(part.get)
    else
      NotFound
  }
    
  /** Download annotations for a specific document as CSV.
    * 
    * @param doc the document ID
    */
  private def backupAnnotations_forDoc(doc: Int)(implicit session: Session) = {
    val gdoc = GeoDocuments.findById(doc)
    if (gdoc.isDefined) {
      val filename = gdoc.get.title.replace(' ', '_').toLowerCase.trim
      val annotations = Annotations.findByGeoDocument(doc)
      val serializer = new CSVSerializer()
      Ok(serializer.asDBBackup(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + ".csv\""))
    } else {
      NotFound
    }
  } 
  
  /** Download annotations for a specific document part as CSV.
    * 
    * @param part the document part ID
    */
  private def backupAnnotations_forPart(part: Int)(implicit session: Session) = {
    val gdocPart = GeoDocumentParts.findById(part)
    if (gdocPart.isDefined) {
      val gdoc = GeoDocuments.findById(gdocPart.get.gdocId)
      val filename = gdoc.get.title.replace(' ', '_').toLowerCase + "_" + gdocPart.get.title.replace(' ', '_').toLowerCase.trim
      val annotations = Annotations.findByGeoDocumentPart(part)
      val serializer = new CSVSerializer()
      Ok(serializer.asDBBackup(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\".csv"))
    } else {
      NotFound 
    }
  }
        
  /** Import annotations into the document with the specified ID.
    * 
    * Annotations are to be delivered as a CSV file in the body of the POST request.
    * @param doc the document ID
    */
  def importAnnotations(doc: Int) = DBAction(parse.multipartFormData) { implicit session =>
    val gdoc = GeoDocuments.findById(doc)
    if (gdoc.isDefined) {
      session.request.body.file("csv").map(filePart => {
        val parser = new CSVParser()
        val annotations = parser.parse(filePart.ref.file.getAbsolutePath, gdoc.get.id.get)
        Logger.info("Importing " + annotations.size + " annotations to " + gdoc.get.title)
        Annotations.insertAll(annotations:_*)
      })
    }
    Redirect(routes.AdminController.index)
  }
  
  /** Drops annotations for the document with the specified ID.
    * 
    * @param doc the document ID 
    */
  def dropAnnotations(doc: Int) = adminAction { username => implicit session =>
    Annotations.deleteForGeoDocument(doc)
    Redirect(routes.AdminController.index)
  }
  
}