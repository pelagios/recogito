package controllers

import play.api.db.slick._
import play.api.mvc.Controller
import models.{ Annotations, GeoDocumentTexts }

object Fulltext extends Controller with Secured {
  
  def index() = dbSessionWithAuth { username => implicit session =>
    val text = GeoDocumentTexts.findById(0).get
    val annotations = Annotations.findByGeoDocumentPart(text.gdocPartId.get)
    val string = new String(text.text, "ISO-8859-1")
    
    val ranges = annotations.map(annotation => {
      (annotation.offset.get, annotation.toponym.get.size)
    })
    
    Ok(views.html.fulltext(string))
  }
  
}