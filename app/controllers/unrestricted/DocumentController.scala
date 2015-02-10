package controllers.unrestricted

import controllers.common.auth.{ Secure, Secured }
import java.util.UUID
import models._
import models.content._
import models.stats.CompletionStats
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }

object DocumentController extends Controller with Secured {
  
  def showMap(doc: Int) = DBAction { implicit rs =>
    val document = GeoDocuments.findById(doc)
    if (document.isDefined) {
      // Source URL of the document or it's first part
      val source = {
        if (document.get.source.isDefined)
          document.get.source
        else
          GeoDocumentParts.findByGeoDocument(doc).flatMap(_.source).headOption
      }
      
      // Base stats
      val verified = Annotations.countForGeoDocumentAndStatus(doc, AnnotationStatus.VERIFIED)
      val places = Annotations.countUniquePlaces(doc) 
      val contributors = Annotations.getContributorStats(doc)
      val inCollection = CollectionMemberships.findForGeoDocument(doc).headOption
      
      Ok(views.html.publicMap(document.get, source, verified, places, contributors.map(_._1), inCollection))
    } else {
      NotFound
    }
  }
  
  def showStats(docId: Int) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(docId)
    if (doc.isDefined) {        
      val (completionStats, untranscribed) = Annotations.getCompletionStats(docId).getOrElse((CompletionStats.empty, 0))
      val autoAnnotationStats = Annotations.getAutoAnnotationStats(docId)
      val unidentifiedToponyms = Annotations.getUnidentifiableToponyms(docId)
      val placeStats = Annotations.getPlaceStats(docId)
      val userStats = Annotations.getContributorStats(docId)
  
      Ok(views.html.stats.documentStats(doc.get, GeoDocumentContent.findByGeoDocument(docId), completionStats, untranscribed, autoAnnotationStats, userStats, unidentifiedToponyms, placeStats, currentUser.map(_.username)))
    } else {
      NotFound
    }
  }
  
  def redirectToFirstText(gdocId: Int) = Action {
    // TODO
    Ok("")
  }
  
  def redirectToFirstImage(gdocId: Int) = Action {
    // TODO
    Ok("")
  } 
  
  def showText(textId: Int) = Action {
    // TODO
    Ok("")
  }
  
  def showImage(id: Int) = DBAction { implicit rs =>
    val image = GeoDocumentImages.findById(id)
    if (image.isDefined) {
      val gdoc = GeoDocuments.findById(image.get.gdocId)
      if (gdoc.get.hasOpenLicense) {
        val gdocPart = image.get.gdocPartId.flatMap(GeoDocumentParts.findById(_))
        Ok(views.html.publicImage(image.get, gdoc.get, gdocPart))
      } else {
        Forbidden("Not Authorized")
      }
    } else { 
      NotFound
    }
  }
  
  /** A shorthand to open a document annotation view (text or image), with a specific annotation highlighted.
    *
    * TODO this should also be available to non-logged in users. Remove Secured trait after fixing this.
    */
  def showAnnotation(id: String) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request =>
    val annotation = Annotations.findByUUID(UUID.fromString(id))
    if (annotation.isDefined) {
      val gdocId = annotation.get.gdocId.get
      val gdocPartId = annotation.get.gdocPartId
      val content = 
        if (gdocPartId.isDefined)
          // Content for a specific part
          GeoDocumentContent.findByGeoDocumentPart(gdocPartId.get)
        else
          // Content attached DIRECTLY to a GeoDocument (not parts of it)
          GeoDocumentContent.findByGeoDocument(gdocId).headOption.map(_._1)
        
      if (content.isDefined)  {
        content.get match {
          case c: GeoDocumentText => Redirect(controllers.tools.routes.TextAnnotationController.showTextAnnotationUI(c.id.get).url + "#" + id)
          case c: GeoDocumentImage => Redirect(controllers.tools.routes.ImageAnnotationController.showImageAnnotationUI(c.id.get).url + "#" + id)
        }
      } else {
        NotFound
      }
    } else {
      NotFound
    }
  }
  
}
