package controllers.admin

import controllers.common.auth.Secured
import controllers.common.io.{ CSVSerializer, ZipExporter }
import models.{ GeoDocuments, Annotations, CollectionMemberships }
import play.api.Logger
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, Controller }
import scala.concurrent.Future
import scala.io.Source

object ExportsController extends Controller with Secured {
  
  private val SEPARATOR = ";"
  
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.exports(CollectionMemberships.listCollections))
  }
  
  def placeStats(collection: String) = adminAction { username => implicit session =>
    val docIds = CollectionMemberships.getGeoDocumentsInCollection(collection)
    val filename = "annotated-places-" + collection.toLowerCase().replace(" ", "-") + ".csv"
    val csv = new CSVSerializer().serializePlaceStats(Annotations.getPlaceStats(docIds))
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> { "attachment; filename=" + filename })
  }
  
  def fullExport = Action.async { implicit request =>
    val user = DB.withSession { implicit s: Session => currentUser }
    if (user.isDefined && user.get.isAdmin) {
      DB.withSession { implicit s: Session =>
        // This is a long-running operation
        val future = Future {
          val file = new ZipExporter().exportGDocs(GeoDocuments.listAll, true)
          val bytes = Source.fromFile(file.file)(scala.io.Codec.ISO8859).map(_.toByte).toArray
          file.finalize()
          bytes
        }
   
        future.map(bytes => Ok(bytes).withHeaders(CONTENT_TYPE -> "application/zip", CONTENT_DISPOSITION -> ({ "attachment; filename=recogito-full.zip" })))
      }
    } else {
      Logger.info("Unauthorized document dump attempt")
      Future { }.map(_ => Forbidden)
    }
  }

}