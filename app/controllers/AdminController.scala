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
import controllers.io.ZipExporter
import scala.io.Source
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import controllers.io.CSVSerializer
import controllers.io.CSVSerializer
import controllers.io.CSVSerializer

/** Administration features.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AdminController extends Controller with Secured {
    
  /** Admin index page **/
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin())
  }
  
  /** Download one specific document (with text and annotations) as a ZIP file **/
  def downloadDocument(gdocId: Int) = adminAction { username => implicit session =>
    val gdoc = GeoDocuments.findById(gdocId)
    if (gdoc.isDefined) {
      val exporter = new ZipExporter()
      val file = exporter.exportGDoc(gdoc.get)
      val bytes = Source.fromFile(file.file)(scala.io.Codec.ISO8859).map(_.toByte).toArray
      file.finalize()
      Ok(bytes).withHeaders(CONTENT_TYPE -> "application/zip", CONTENT_DISPOSITION -> ({ "attachment; filename=" + exporter.escapeTitle(gdoc.get.title) }))
    } else {
      NotFound
    }
  }
  
  /** Download all documents (with text and annotations) as a ZIP file **/
  def downloadAllDocuments = Action.async { implicit request =>
    val user = DB.withSession { implicit s: Session => currentUser }
    if (user.isDefined && user.get.isAdmin) {
      DB.withSession { implicit s: Session =>
        // This is a long-running operation
        val future = Future {
          val file = new ZipExporter().exportGDocs(GeoDocuments.listAll)
          val bytes = Source.fromFile(file.file)(scala.io.Codec.ISO8859).map(_.toByte).toArray
          file.finalize()
          bytes
        }
   
        future.map(bytes => Ok(bytes).withHeaders(CONTENT_TYPE -> "application/zip", CONTENT_DISPOSITION -> ({ "attachment; filename=all.zip" })))
      }
    } else {
      Logger.info("Unauthorized document backup attempt")
      Future { }.map(_ => Forbidden)
    }
  }
  
  /** Upload documents (with text and annotations) to the database, from a ZIP file **/
  def uploadDocuments = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("zip").map(filePart => ZipImporter.importZip(new ZipFile(filePart.ref.file)))
      Redirect(routes.AdminController.index)
    } else {
      BadRequest
    }
  }
    
  /** Delete a document (and associated data) from the database **/
  def deleteDocument(id: Int) = protectedDBAction(Secure.REJECT) { username => implicit session =>
    Annotations.deleteForGeoDocument(id)
    GeoDocumentTexts.deleteForGeoDocument(id)
    GeoDocumentParts.deleteForGeoDocument(id)    
    GeoDocuments.delete(id)
    Status(200)
  }
        
  /** Import annotations from a CSV file into the document with the specified ID **/
  def uploadAnnotations(doc: Int) = DBAction(parse.multipartFormData) { implicit session =>
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
  
  /** Drop all annotations from the document with the specified ID **/
  def deleteAnnotations(doc: Int) = adminAction { username => implicit session =>
    Annotations.deleteForGeoDocument(doc)
    Redirect(routes.AdminController.index)
  }
  
  /** Dowload all user data for the purpose of backup **/
  def downloadUsers = adminAction { username => implicit session =>
    val csv = new CSVSerializer().serializeUsers(Users.listAll)
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-users.csv")
  }
  
  /** Dowload the edit history for the purpose of backup **/
  def downloadEditHistory = Action.async { implicit request =>
    val user = DB.withSession { implicit s: Session => currentUser }
    if (user.isDefined && user.get.isAdmin) {
      DB.withSession { implicit s: Session =>
        // This is a long-running operation
        val future = Future {
          new CSVSerializer().serializeEditHistory(EditHistory.getAll)
        }
        
        future.map(csv => Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-history.csv"))
      }  
    } else {
      Logger.info("Unauthorized user backup attempt")
      Future { }.map(_ => Forbidden)      
    }
  }
  
  def downloadStatsTimeline = adminAction { username => implicit session =>
    val csv = new CSVSerializer().serializeStats(StatsHistory.listAll)
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-stats-timeline.csv")
  }
  
}