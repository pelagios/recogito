package controllers.api

import controllers.common.auth.{ Secure, Secured }
import controllers.common.io.{ CSVSerializer, JSONSerializer }
import models._
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.{ Json, JsObject }
import play.api.Play.current
import org.pelagios.Scalagios
import org.pelagios.api.annotation.{ AnnotatedThing, Annotation => OAnnotation, Transcription, TranscriptionType, SpecificResource }
import java.io.ByteArrayOutputStream
import play.api.Logger
import org.pelagios.api.annotation.selector.TextOffsetSelector
import scala.io.Source
import play.api.mvc.AnyContent

/** GeoDocument JSON API.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>  
  */
object DocumentAPIController extends Controller with Secured {
  
  private val CSV = "csv"
  private val RDF_XML = "rdfxml"
  private val UTF8 = "UTF-8"

  /** Returns the data for a specific document in the specified format.
    *
    * The format parameter supports either 'json' or 'csv' (case-insensitive). If
    * neither is provided, or no format is provided at all, the format defaults to
    * JSON.
    * @param id the document ID
    * @param the format
    */
  def get(id: String) = DBAction { implicit session =>
    val (idInt, format) = id match {
      case id if id.endsWith(".csv") => (id.substring(0, id.lastIndexOf(".")).toInt, CSV)
      case id if id.endsWith(".rdf") => (id.substring(0, id.lastIndexOf(".")).toInt, RDF_XML)
      case _ => (id.toInt, JSON)
    }
    
    val doc = GeoDocuments.findById(idInt)
    if (doc.isDefined) {
      if (format.equalsIgnoreCase(CSV))
        Redirect(controllers.unrestricted.routes.DownloadController.downloadCSV(doc.get.id.get.toString))
      else if (format.equalsIgnoreCase(RDF_XML))
        get_RDF(doc.get, Scalagios.RDFXML, controllers.frontpage.routes.FrontPageController.index(None).absoluteURL(false))
      else
        get_JSON(doc.get)
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))            
    }
  }
  
  private def get_RDF(doc: GeoDocument, format: String, basePath: String)(implicit session: Session) = {
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
    Ok(new JSONSerializer().toJson(doc, true))
    
  def signOff(textId: Option[Int], imageId: Option[Int]) = protectedDBAction(Secure.REJECT) { username => implicit request =>
    if (textId.isDefined) {
      val newStatus = SignOffs.toggleStatusForText(textId.get, username)
      Ok(Json.parse("{ \"success\": true, \"signed_off\": " + newStatus + " }"))
    } else if (imageId.isDefined) {
      val newStatus = SignOffs.toggleStatusForImage(imageId.get, username)
      Ok(Json.parse("{ \"success\": true, \"signed_off\": " + newStatus + " }"))
    } else {
      NotFound
    }
  }
    
}
