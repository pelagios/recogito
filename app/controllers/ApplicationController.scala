package controllers

import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import play.api.Logger
import org.pelagios.gazetteer.GazetteerUtils

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
  def showMap(doc: Int) = DBAction { implicit rs =>
    val document = GeoDocuments.findById(doc)
    if (document.isDefined)
      Ok(views.html.public_map(document.get))
    else
      NotFound
  }
    
  /** Shows the text annotation UI for the specified text.
    * 
    * @param text the internal ID of the text in the DB 
    */
  def showTextAnnotationUI(text: Int) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request => 
    val gdocText = GeoDocumentTexts.findById(text)
    if (gdocText.isDefined) {
      val plaintext = new String(gdocText.get.text, UTF8)
      val annotations = if (gdocText.get.gdocPartId.isDefined) {
          Annotations.findByGeoDocumentPart(gdocText.get.gdocPartId.get)
        } else {
          Annotations.findByGeoDocument(gdocText.get.gdocId)
        }
      
      // Build HTML
      val ranges = annotations.foldLeft(("", 0)) { case ((markup, beginIndex), annotation) => {
        if (annotation.status == AnnotationStatus.FALSE_DETECTION) {
          (markup, beginIndex)
        } else {
          // Use corrections if they exist, or Geoparser results otherwise
          val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
          val offset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset else annotation.offset 
          val url = if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.trim.isEmpty) 
                      annotation.correctedGazetteerURI
                    else annotation.gazetteerURI

          if (offset.isDefined && offset.get < beginIndex)
            debugTextAnnotationUI(annotation)
          
          val cssClassA = annotation.status match {
            case AnnotationStatus.VERIFIED => "annotation verified"
            case AnnotationStatus.IGNORE => "annotation ignore"
            case AnnotationStatus.NOT_IDENTIFYABLE => "annotation not-identifyable"
            case _ => "annotation" 
          }
          
          val cssClassB = if (annotation.correctedToponym.isDefined) " manual" else " automatic"
   
          val title = "#" + annotation.id.get + " " +
            AnnotationStatus.screenName(annotation.status) + " (" +
            { if (annotation.correctedToponym.isDefined) "Manual Correction" else "Automatic Match" } +
            ")"
            
          if (toponym.isDefined && offset.isDefined) {
            val nextSegment = plaintext.substring(beginIndex, offset.get) +
              "<a href=\"" + url.map(GazetteerUtils.normalizeURI(_)).getOrElse("#") + "\" data-id=\"" + annotation.id.get + "\" class=\"" + cssClassA + cssClassB + "\" title=\"" + title + "\">" + toponym.get + "</a>"
              
            (markup + nextSegment, offset.get + toponym.get.size)
          } else {
            (markup, beginIndex)
          }
        }
      }}

      val html = (ranges._1 + plaintext.substring(ranges._2)).replace("\n", "<br/>")
      
      val gdoc = GeoDocuments.findById(gdocText.get.gdocId)
      val gdocPart = gdocText.get.gdocPartId.map(id => GeoDocumentParts.findById(id)).flatten
      
      val title = gdoc.get.title + gdocPart.map(" - " + _.title).getOrElse("")
      Ok(views.html.text_annotation(title, html, gdoc.get.id.get, gdocPart.map(_.id.get)))
    } else {
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }")) 
    }
  }
  
  /** Helper method that generates detailed debug output for overlapping annotations.
    * 
    * @param annotation the offending annotation
    */
  private def debugTextAnnotationUI(annotation: Annotation)(implicit s: Session) = {
    val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
    Logger.error("Offending annotation: #" + annotation.id.get + " - " + toponym.getOrElse(""))
    Annotations.getOverlappingAnnotations(annotation).foreach(a => Logger.error("Overlaps with: #" + a.id.get))
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