package controllers.tools

import controllers.common.auth.{ Secure, Secured }
import models.{ GeoDocuments, GeoDocumentContent, GeoDocumentParts, SignOffs }
import models.content.GeoDocumentImages
import play.api.db.slick._
import play.api.mvc.Controller

object ImageAnnotationController extends Controller with Secured {
  
  def showImageAnnotationUI(imageId: Int) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit session =>
    val gdocImage = GeoDocumentImages.findById(imageId)
    if (gdocImage.isDefined) {
      val gdoc = GeoDocuments.findById(gdocImage.get.gdocId)
      val gdocPart = gdocImage.get.gdocPartId.flatMap(GeoDocumentParts.findById(_))
      val allImages = GeoDocumentContent.findByGeoDocument(gdoc.get.id.get)      
      val signOffs = SignOffs.findForGeoDocumentImage(gdocImage.get.id.get).map(_._1)
      
      Ok(views.html.imageAnnotation(gdocImage.get, gdoc.get, gdocPart, allImages, username, signOffs.contains(username), signOffs))
    } else {
      NotFound
    }  
  }  
  
}
