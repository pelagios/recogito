package controllers

import play.api.db.slick._
import play.api.mvc.Controller
import models.{ Annotations, GeoDocumentTexts }
import models.AnnotationStatus
import play.api.Logger

object Fulltext extends Controller with Secured {
  
  def index(textId: Int) = dbSessionWithAuth { username => implicit session =>
    val text = GeoDocumentTexts.findById(textId).get
    val annotations = Annotations.findByGeoDocumentPart(text.gdocPartId.get)
    val string = new String(text.text, "UTF-8")
          
    val ranges = annotations.foldLeft(("", 0)) { case ((result, beginIndex), annotation) => {
      if (annotation.status == AnnotationStatus.FALSE_DETECTION) {
        (result, beginIndex)
      } else {
        val offset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset.get else annotation.offset.get      
        val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym.get else annotation.toponym.get
        val cssClass = if (annotation.correctedToponym.isDefined) "annotation corrected" else if (annotation.status == AnnotationStatus.VERIFIED) "annotation verified" else "annotation"
   
        if (offset != null && toponym != null) {
          val rs = result + string.substring(beginIndex, offset) + "<span data-id=\"" + annotation.id.get + "\" class=\"" + cssClass + "\">" + toponym + "</span>"
          (rs, offset + toponym.size)
        } else {
          (result, beginIndex)
        }
      }
    }}
    
    Ok(views.html.fulltext(ranges._1.replace("\n", "<br/>")))
  }
  
}
