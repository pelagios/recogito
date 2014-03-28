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
import com.fasterxml.jackson.core.JsonParseException

/** Administration features.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AdminController extends Controller with Secured {
    
  /** Admin index page **/
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.index())
  }
  
  /** Admin users page **/
  def backup = adminAction { username => implicit session =>
    Ok(views.html.admin.backup())
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
      try {
        val f = formData.get.file("zip")
        if (f.isDefined) {
          val zipFile = new ZipFile(f.get.ref.file)
          val errors = ZipImporter.validateZip(zipFile)
          if (errors.size == 0) {
            val imported = ZipImporter.importZip(zipFile)
            Redirect(routes.AdminController.backup).flashing("success" -> { "Uploaded " + imported + " document(s)." })
          } else {
            Redirect(routes.AdminController.backup).flashing("error" -> { "<strong>Your file had " + errors.size + " errors:</strong>" + errors.map("<p>" + _ + "</p>").mkString("\n") })
          }
        } else {
          BadRequest
        }
      } catch {
        case t: Throwable => Redirect(routes.AdminController.backup).flashing("error" -> { "There is something wrong with your JSON: " + t.getMessage })
      }
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
        val annotations = parser.parseAnnotations(filePart.ref.file.getAbsolutePath, gdoc.get.id.get)
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
  
  /** Batch-upload users to the database, for the purpose of restoring **/
  def uploadUsers = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("csv").map(filePart => {
        val users = new CSVParser().parseUsers(filePart.ref.file.getAbsolutePath)
        Users.insertAll(users:_*)
      })
      Redirect(routes.AdminController.backup)
    } else {
      BadRequest
    }
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
  
  def uploadEditHistory = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("csv").map(filePart => {
        val history = new CSVParser().parseEditHistory(filePart.ref.file.getAbsolutePath)
        EditHistory.insertAll(history:_*)
      })
      Redirect(routes.AdminController.backup)
    } else {
      BadRequest
    }
  }
  
  def downloadStatsTimeline = adminAction { username => implicit session =>
    val csv = new CSVSerializer().serializeStats(StatsHistory.listAll)
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-stats-timeline.csv")
  }
  
  def uploadStatsTimeline = adminAction { username => implicit session =>
    Redirect(routes.AdminController.backup)
  }
  
}