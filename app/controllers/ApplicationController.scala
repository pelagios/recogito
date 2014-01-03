package controllers

import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }

/** Main application entrypoint.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object ApplicationController extends Controller with Secured {
  
  private val UTF8 = "UTF-8"
    
  /** Returns the index page for logged-in users **/
  def index = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request => 
    Ok(views.html.index(username)) 
  }
   
  /** Shows the 'public map' for the specified document.
    *  
    * @param doc the document ID 
    */  
  def showMap(doc: Int) = Action {
    Ok(views.html.map_public(doc))
  }
    
  /** Shows the text annotation UI for the specified text.
    * 
    * @param text the internal ID of the text in the DB 
    */
  def showTextAnnotationUI(text: Int) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request => 
    val gdocText = GeoDocumentTexts.findById(text)
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

  /** Shows the map-based georesolution correction UI for the specified document.
    *
    * @param doc the document ID 
    */
  def showMapCorrectionUI(doc: Int) = protectedAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request => 
    Ok(views.html.map_correction(username, doc))
  }
  
  /** Shows the edit history overview page **/
  def showHistory() = DBAction { implicit session =>
    // TODO just a dummy for now
    Ok(views.html.edit_history(EditHistory.getLastN(500))) 
  }

}