package controllers.admin

import controllers.{ Secure, Secured }
import controllers.common.io.{CSVParser, CSVSerializer, ZipExporter, ZipImporter}
import java.util.zip.ZipFile
import models._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source

/** Administration features.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AdminController extends Controller with Secured {
    
  /** Admin index page **/
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.index())
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
  
}
