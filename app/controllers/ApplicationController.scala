package controllers

import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import play.api.Logger

/** Main application entrypoint.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object ApplicationController extends Controller with Secured with CTSClient {
  
  private val UTF8 = "UTF-8"
    
  /** Returns the index page for logged-in users **/
  def index(collection: Option[String]) = DBAction { implicit rs =>    
    if (collection.isEmpty) {
      // If no collection is selected, redirect to the first in the list
      val allCollections = CollectionMemberships.listCollections :+ "other"
      Redirect(routes.ApplicationController.index(Some(allCollections.head.toLowerCase)))
    } else {
      // IDs of all documents NOT assigned to a collection
      val unassigned = CollectionMemberships.getUnassignedGeoDocuments
        
      // Documents for the selected collection
      val gdocs = 
        if (collection.get.equalsIgnoreCase("other"))
          GeoDocuments.findAll(unassigned)
        else
          GeoDocuments.findAll(CollectionMemberships.getDocumentsInCollection(collection.get))
                  
      // The information required for the collection selection widget
      val docsPerCollection = CollectionMemberships.listCollections.map(collection =>
        (collection, CollectionMemberships.countDocumentsInCollection(collection))) ++
        { if (unassigned.size > 0) Seq(("Other", unassigned.size)) else Seq.empty[(String, Int)] }
            
      // Populate the correct template, depending on login state
      if (currentUser.isDefined && isAuthorized) {      
        Ok(views.html.index(gdocs, docsPerCollection, collection.get, currentUser.get))
      } else {
        val sorted = gdocs
          .sortBy(d => (d.date, d.author, d.title))
          .map(doc => (doc, doc.countTotalToponyms, doc.countUnverifiedToponyms))
          
        Ok(views.html.index_public(sorted, docsPerCollection, collection.get))
      }    
    }
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
  def showTextAnnotationUI(text: Option[Int], ctsURI: Option[String]) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit request =>   
    // Warning: temporary (d'oh) hack for supporting CTS alongside stored texts
    val (someGDocText, somePlaintext, someAnnotations) = if (ctsURI.isDefined) {
      val annotations = Annotations.findBySource(ctsURI.get)
      (None, Some(getPlaintext(ctsURI.get)), Some(annotations))
    } else {
      val gdocText = GeoDocumentTexts.findById(text.get)
      val a = gdocText.map(txt => {
        if (gdocText.get.gdocPartId.isDefined) {
          Annotations.findByGeoDocumentPart(gdocText.get.gdocPartId.get)
        } else {
          Annotations.findByGeoDocument(gdocText.get.gdocId)
        }
      })
      (gdocText, gdocText.map(txt => new String(txt.text, UTF8)), a)
    }
    
    if (somePlaintext.isDefined && someAnnotations.isDefined) {
      val gdoc = someGDocText.map(text => GeoDocuments.findById(text.gdocId)).flatten
      val gdocPart = someGDocText.map(text => text.gdocPartId.map(id => GeoDocumentParts.findById(id))).flatten.flatten
          
      val texts = gdoc.map(doc => textsForGeoDocument(doc.id.get)).getOrElse(Seq.empty[(GeoDocumentText, Option[String])])
      // val title = gdoc.get.title s gdocPart.map(" - " + _.title).getOrElse("")      
      
      val html = buildHTML(somePlaintext.get, someAnnotations.get)
      
      Ok(views.html.annotation(gdoc, gdoc.map(gdoc => textsForGeoDocument(gdoc.id.get)).getOrElse(Seq.empty[(models.GeoDocumentText, Option[String])]), username, gdocPart, html, ctsURI))
    } else {
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }")) 
    }
  }

  private def buildHTML(plaintext: String, annotations: Seq[Annotation])(implicit session: Session): String = {
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
          case AnnotationStatus.NO_SUITABLE_MATCH => "annotation not-identifyable"
          case AnnotationStatus.AMBIGUOUS => "annotation not-identifyable"
          case AnnotationStatus.MULTIPLE => "annotation not-identifyable"
          case AnnotationStatus.NOT_IDENTIFYABLE => "annotation not-identifyable"
          case _ => "annotation" 
        }
          
        val cssClassB = if (annotation.correctedToponym.isDefined) " manual" else " automatic"
   
        val title = "#" + annotation.uuid + " " +
            AnnotationStatus.screenName(annotation.status) + " (" +
          { if (annotation.correctedToponym.isDefined) "Manual Correction" else "Automatic Match" } +
            ")"
            
        if (toponym.isDefined && offset.isDefined) {
          val nextSegment = escapePlaintext(plaintext.substring(beginIndex, offset.get)) +
            "<span data-id=\"" + annotation.uuid + "\" class=\"" + cssClassA + cssClassB + "\" title=\"" + title + "\">" + escapePlaintext(toponym.get) + "</span>"
              
          (markup + nextSegment, offset.get + toponym.get.size)
        } else {
          (markup, beginIndex)
        }
      }
    }}
 
    (ranges._1 + escapePlaintext(plaintext.substring(ranges._2))).replace("\n", "<br/>") 
  }
  
  private def escapePlaintext(segment: String): String = {
    // Should cover most cases (?) - otherwise switch to Apache Commons StringEscapeUtils
    segment
      .replace("<", "&lt;")
      .replace(">", "&gt;")
  }
  
  /** Helper method that generates detailed debug output for overlapping annotations.
    * 
    * @param annotation the offending annotation
    */
  private def debugTextAnnotationUI(annotation: Annotation)(implicit s: Session) = {
    val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
    Logger.error("Offending annotation: #" + annotation.uuid + " - " + annotation)
    Annotations.getOverlappingAnnotations(annotation).foreach(a => Logger.error("Overlaps with: #" + a.uuid))
  }

  /** Shows the map-based georesolution correction UI for the specified document.
    *
    * @param doc the document ID 
    */
  def showGeoResolutionUI(docId: Int) = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit session => 
    val doc = GeoDocuments.findById(docId)
    if (doc.isDefined)
      Ok(views.html.georesolution(doc.get, textsForGeoDocument(docId), username))
    else
      NotFound
  }
  
  /** Shows detailed stats for a specific document **/
  def showDocumentStats(docId: Int) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(docId)
    if (doc.isDefined) {        
      Ok(views.html.document_stats(doc.get, textsForGeoDocument(docId), currentUser.map(_.username)))
    } else {
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Document not found\" }"))
    }
  }
  
  /** Shows the edit history overview page **/
  def showHistory() = DBAction { implicit session =>
    // TODO just a dummy for now
    val history = EditHistory.getLastN(200).map(event => (event, Annotations.findByUUID(event.annotationId).flatMap(_.gdocId)))
    Ok(views.html.edit_history(history)) 
  }
  
  /** Shows the stats history page **/
  def showStats() = DBAction { implicit session =>
    // TODO just a dummy for now
    Ok(views.html.stats(StatsHistory.listAll())) 
  }
  
  /** Helper method to get the texts (and titles) for a specific GeoDocument **/
  private def textsForGeoDocument(docId: Int)(implicit session: Session): Seq[(GeoDocumentText, Option[String])] =
    GeoDocumentTexts.findByGeoDocument(docId).map(text =>
        // If the text is associated with a GDoc part (rather than the GDoc directly), we'll fetch the part title
        (text, text.gdocPartId.map(partId => GeoDocumentParts.findById(partId).map(_.title)).flatten))
    

}