package controllers.tools

import models.{ GeoDocuments, GeoDocumentContent }
import controllers.common.auth.{ Secure, Secured }
import play.api.db.slick._
import play.api.mvc.Controller

object GeoResolutionController extends Controller with Secured {
  
  def showGeoResolutionUI(docId: Int) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit session => 
    val doc = GeoDocuments.findById(docId)
    if (doc.isDefined)
      Ok(views.html.geoResolution(doc.get, GeoDocumentContent.findByGeoDocument(docId), username))
    else
      NotFound
  }  
  
}
