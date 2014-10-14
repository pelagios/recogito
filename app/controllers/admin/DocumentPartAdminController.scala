package controllers.admin

import controllers.Secured
import models.{ GeoDocumentPart, GeoDocumentParts }
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._

object DocumentPartAdminController extends Controller with Secured {
  
  /** The document form (used in both create and edit documents) **/
  private val documentPartForm = Form(
	mapping(
		"id" -> optional(number),
		"gdocId" -> number,
		"title" -> text, 
		"source" -> optional(text) 
	)(GeoDocumentPart.apply)(GeoDocumentPart.unapply)
  )
  
  def editDocumentPart(id: Int) = adminAction { username => implicit session =>
    GeoDocumentParts.findById(id).map { part =>
      Ok(views.html.admin.documentPartDetails(id, part.gdocId, documentPartForm.fill(part)))
    }.getOrElse(NotFound)
  }
  
  def updateDocumentPart(id: Int) = adminAction { username => implicit session =>
    val gdocPart = GeoDocumentParts.findById(id)
    if (gdocPart.isDefined) {
      documentPartForm.bindFromRequest.fold(
        formWithErrors => {
	      BadRequest(views.html.admin.documentPartDetails(id, gdocPart.get.gdocId, formWithErrors))
	    },
	  
        part => {
          GeoDocumentParts.update(part)
          Redirect(routes.DocumentAdminController.editDocument(gdocPart.get.gdocId))
        })
    } else {
      NotFound
    }
  }

}