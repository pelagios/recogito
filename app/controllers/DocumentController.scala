package controllers

import controllers.common.io.{ CSVSerializer, JSONSerializer }
import models._
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.{ Json, JsObject }
import play.api.Play.current
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios
import org.pelagios.api.annotation.{ AnnotatedThing, Annotation => OAnnotation, Transcription, TranscriptionType, SpecificResource }
import java.io.ByteArrayOutputStream
import play.api.Logger
import org.pelagios.api.annotation.selector.TextOffsetSelector

/** GeoDocument JSON API.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>  
  */
object DocumentController extends Controller with Secured {
  
  private val CSV = "csv"
  private val RDF_XML = "rdfxml"
  private val UTF8 = "UTF-8"
  
  /** Returns the list of all geo documents in the database as JSON **/
  def listAll = DBAction { implicit session =>
    val documents = GeoDocuments.listAll().map(doc => Json.obj(
      "id" -> doc.id,
      "title" -> doc.title
    ))
    Ok(Json.toJson(documents))
  }

  /** Returns the JSON data for a specific document in the specified format.
    *
    * The format parameter supports either 'json' or 'csv' (case-insensitive). If
    * neither is provided, or no format is provided at all, the format defaults to
    * JSON.
    * @param id the document ID
    * @param the format
    */
  def get(id: Int, format: Option[String]) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {
      if (format.isDefined && format.get.equalsIgnoreCase(CSV))
        get_CSV(doc.get)
      else if (format.isDefined && format.get.equalsIgnoreCase(RDF_XML))
        get_RDF(doc.get, RDFFormat.RDFXML, routes.ApplicationController.index(None).absoluteURL(false))
      else
        get_JSON(doc.get)
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))            
    }
  }
  
  private def get_CSV(doc: GeoDocument)(implicit session: Session) = {
    val id = doc.id.get
    val annotations = Annotations.findByGeoDocumentAndStatus(id, AnnotationStatus.VERIFIED)
    val serializer = new CSVSerializer()
    
    def escapeTitle(title: String) = 
      title.replace(" ", "_").replace(",", "_")
    
    Ok(serializer.serializeAnnotationsConsolidated(doc, annotations))
      .withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=" + escapeTitle(doc.title) + ".csv"))
  }
  
  private def get_RDF(doc: GeoDocument, format: RDFFormat, basePath: String)(implicit session: Session) = {
    val thing = AnnotatedThing(basePath + "egd", doc.title)
    val annotations = Annotations.findByGeoDocumentAndStatus(doc.id.get, AnnotationStatus.VERIFIED)
    
    // Convert Recogito annotations to OA
    annotations.zipWithIndex.foreach{ case (a, idx) => {
      val place =  { if (a.correctedGazetteerURI.isDefined) a.correctedGazetteerURI else a.gazetteerURI }
        .map(Seq(_)).getOrElse(Seq.empty[String])
        
      val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
      val toponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym
      
      val transcription = toponym.map(t => Transcription(t, TranscriptionType.Toponym))
      val selector = offset.map(offset => TextOffsetSelector(offset, toponym.get.size))
      
      val target = if (selector.isDefined) SpecificResource(thing, selector.get) else thing
          
      val oa = OAnnotation(basePath + "annotations#" + idx, target, place = place, transcription = transcription)
    }}

    val out = new ByteArrayOutputStream()
    Scalagios.writeAnnotations(Seq(thing), out, format)
    Ok(new String(out.toString(UTF8))).withHeaders(CONTENT_TYPE -> "application/rdf+xml", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd.rdf"))      
  }
      
  private def get_JSON(doc: GeoDocument)(implicit s: Session) =
    Ok(JSONSerializer.toJson(doc, true))
  
}