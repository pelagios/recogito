package controllers

import models._
import org.pelagios.grct.exporter.CSVExporter
import org.pelagios.grct.importer.CSVImporter
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc.{ Action, Controller }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.Logger

object AdminController extends Controller with Secured {
  
  def index() = dbSessionWithAuth { username => implicit session => {
    Ok(views.html.admin())
  }}
  
  def downloadPart(gdocPartId: Int) = dbSessionWithAuth { username => implicit session =>
    val gdocPart = GeoDocumentParts.findById(gdocPartId)
    if (gdocPart.isDefined) {
      val gdoc = GeoDocuments.findById(gdocPart.get.gdocId)
      val filename = gdoc.get.title.replace(' ', '_').toLowerCase + "_" + gdocPart.get.title.replace(' ', '_').toLowerCase.trim
      val annotations = Annotations.findByGeoDocumentPart(gdocPartId)
      Ok(CSVExporter.toDump(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\".csv"))
    } else {
      NotFound 
    }
  }
  
  def downloadDocument(gdocId: Int) = dbSessionWithAuth { username => implicit session =>
    val gdoc = GeoDocuments.findById(gdocId)
    if (gdoc.isDefined) {
      val filename = gdoc.get.title.replace(' ', '_').toLowerCase.trim
      val annotations = Annotations.findByGeoDocument(gdocId)
      Ok(CSVExporter.toDump(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + ".csv\""))
    } else {
      NotFound
    }
  }
  
  def dropDocument(gdocId: Int) = dbSessionWithAuth { username => implicit session =>
    Annotations.deleteForGeoDocument(gdocId)
    Redirect(routes.AdminController.index)
  }
  
  def importCSV(gdocId: Int) = DBAction(parse.multipartFormData) { implicit session =>
    val gdoc = GeoDocuments.findById(gdocId)
    if (gdoc.isDefined) {
      session.request.body.file("csv").map(filePart => {
        val annotations = CSVImporter.importCSV(filePart.ref.file.getAbsolutePath, gdoc.get.id.get)
        annotations.foreach(annotation => Annotations.insert(annotation))
      })
    }
    Redirect(routes.AdminController.index)
  }

}