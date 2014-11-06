package controllers.admin

import controllers.Secured
import controllers.common.io.{ CSVSerializer, CSVParser, ZipExporter }
import models._
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source
import models.GlobalStatsHistory

/** Controller for the 'Backup & Restore' section of the admin area.
  * 
  * Includes the methods for batch-down/uploading of all types of data in Recogito.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object BackupRestoreController extends Controller with Secured {

  /** Backup & Restore section index page **/
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.backup())
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
  
  /** Download all user data as CSV **/
  def downloadAllUsers = adminAction { username => implicit session =>
    val csv = new CSVSerializer().serializeUsers(Users.listAll)
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-users.csv")
  }
  
  /** Batch-upload users to the database **/
  def uploadUsers = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("csv").map(filePart => {
        val users = new CSVParser().parseUsers(filePart.ref.file.getAbsolutePath)
        Users.insertAll(users)
      })
      Redirect(routes.BackupRestoreController.index)
    } else {
      BadRequest
    }
  }
  
  /** Download the edit history as CSV **/
  def downloadEditHistory = Action.async { implicit request =>
    val user = DB.withSession { implicit s: Session => currentUser }
    if (user.isDefined && user.get.isAdmin) {
      DB.withSession { implicit s: Session =>
        // This is a long-running operation
        val future = Future {
          new CSVSerializer().serializeEditHistory(EditHistory.listAll)
        }
        
        future.map(csv => Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-history.csv"))
      }  
    } else {
      Logger.info("Unauthorized user backup attempt")
      Future { }.map(_ => Forbidden)      
    }
  }
    
  /** Batch-upload edit history records to the database **/  
  def uploadEditHistory = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("csv").map(filePart => {
        val history = new CSVParser().parseEditHistory(filePart.ref.file.getAbsolutePath)
        EditHistory.insertAll(history)
      })
      Redirect(routes.BackupRestoreController.index)
    } else {
      BadRequest
    }
  }
  
  /** Download the stats timeline data as CSV **/
  def downloadStatsTimeline = adminAction { username => implicit session =>
    val csv = new CSVSerializer().serializeStats(GlobalStatsHistory.listAll)
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> "attachment; filename=recogito-stats-timeline.csv")
  }
  
  /** Batch-upload stats timeline records to the database **/  
  def uploadStatsTimeline = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      formData.get.file("csv").map(filePart => {
        val timeline = new CSVParser().parseStatsTimeline(filePart.ref.file.getAbsolutePath)
        GlobalStatsHistory.insertAll(timeline)
      })
      Redirect(routes.BackupRestoreController.index)
    } else {
      BadRequest
    }
  }
  
}