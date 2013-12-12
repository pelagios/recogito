package controllers

import play.api.db.slick._
import play.api.mvc.Controller
import models.{ Annotations, GeoDocumentTexts }
import models.AnnotationStatus

object Fulltext extends Controller with Secured {
  
  def index() = dbSessionWithAuth { username => implicit session =>
    val text = GeoDocumentTexts.findById(4).get
    val annotations = Annotations.findByGeoDocumentPart(text.gdocPartId.get)
    val string = new String(text.text, "ISO-8859-1")
    
    val ranges = annotations.foldLeft(("", 0)) { case ((result, beginIndex), annotation) => {
      val offset = annotation.offset
      val toponym = annotation.toponym
      val color = if (annotation.status == AnnotationStatus.VERIFIED) "#77ff77;" else "#aaa;"
      if (offset.isDefined && toponym.isDefined) {
        val rs = result + string.substring(beginIndex, offset.get) + "<span style=\"background-color:" + color + "\">" + toponym.get + "</span>"
        (rs, offset.get + toponym.get.size)
      } else {
        (result, beginIndex)
      }
    }}
    
    Ok(views.html.fulltext(ranges._1.replace("\n", "<br/>")))
  }
  
}
