package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc.{ Action, Controller }
import scala.io.Source
import org.pelagios.grct.importer.CSVImporter
import models.Annotations
import play.api.db.slick.DBAction
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

object Admin extends Controller with Secured {
  
  def index() = dbSessionWithAuth { username => implicit session => {
    Ok(views.html.admin())
  }}
  
  def uploadEGDPart() = DBAction(parse.multipartFormData) { implicit session =>
    val part = session.request.body.dataParts.toMap.get("egd_part")
    if (part.isDefined && part.get.size == 1) {
      session.request.body.file("csv").map { egdPart =>
        CSVImporter.importAnnotations(egdPart.ref.file.getAbsolutePath, 0, part.get(0).toInt).foreach(annotation => Annotations.insert(annotation))
      }     
    }
    
    Ok("")
  }

}