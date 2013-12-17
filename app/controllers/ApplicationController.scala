package controllers

import models._
import play.api.db.slick._
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import play.api.Play.current
import org.pelagios.grct.exporter.CSVExporter
import play.api.Logger

/** Main application entrypoint 
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object ApplicationController extends Controller with Secured {
  
  private val UTF8 = "UTF-8"
  
  /** Returns the index page for logged-in users **/
  def index = dbSessionWithAuth { username => implicit request => 
    Ok(views.html.index(username)) 
  }
  
  /** Returns the map view for a specified document **/
  def map(docId: Int) = withAuth { username => implicit request => 
    Ok(views.html.map(username, docId))
  }
  
  /** Returns the text annotation view for a specified text **/
  def text(textId: Int) = dbSessionWithAuth { username => implicit request => 
    val gdocText = GeoDocumentTexts.findById(textId)
    if (gdocText.isDefined) {
      val annotations = Annotations.findByGeoDocumentPart(gdocText.get.gdocPartId.get)
      val plaintext = new String(gdocText.get.text, UTF8)
      
      // Build HTML
      val ranges = annotations.foldLeft(("", 0)) { case ((markup, beginIndex), annotation) => {
        if (annotation.status == AnnotationStatus.FALSE_DETECTION) {
          // Not shown in text annotation UI at all
          (markup, beginIndex)
        } else {
          // Use corrections if they exist, or Geoparser results otherwise
          val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
          val offset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset else annotation.offset      
          val cssClass = if (annotation.correctedToponym.isDefined) 
                             "annotation corrected"
                         else if (annotation.status == AnnotationStatus.VERIFIED)
                             "annotation verified"
                         else "annotation"
   
          if (toponym.isDefined && offset.isDefined) {
            val nextSegment = plaintext.substring(beginIndex, offset.get) +
              "<span data-id=\"" + annotation.id.get + "\" class=\"" + cssClass + "\">" + toponym.get + "</span>"
              
            (markup + nextSegment, offset.get + toponym.get.size)
          } else {
            (markup, beginIndex)
          }
        }
      }}
      
      val html = (ranges._1 + plaintext.substring(ranges._2)).replace("\n", "<br/>")
      Ok(views.html.text_annotation(html))
    } else {
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }")) 
    }
  }
  
  def csv(gdocId: Int) = DBAction { implicit session => 
    val parts = GeoDocumentParts.findByGeoDocument(gdocId)
    val annotations = parts.map(part => Annotations.findByGeoDocumentPart(part.id.get)).flatten
    Ok(CSVExporter.toCSV(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd-" + gdocId.toString + ".csv"))
  }

}