package controllers.unrestricted

import controllers.common.io.{ CSVSerializer, TextSerializer }
import models.{ Annotations, AnnotationStatus, CollectionMemberships, GeoDocuments }
import models.content.GeoDocumentTexts
import play.api.db.slick._
import play.api.mvc.Controller
import scala.io.Source

object DownloadController extends Controller {
  
  private val DOT_CSV = ".csv"
  
  private val voidTemplate =
    Source.fromFile("conf/void-template.ttl").getLines().takeWhile(!_.trim.startsWith(".")).mkString("\n") + "\n"

  private def escapeTitle(title: String) = 
    title.replace(" ", "_").replace(",", "_")
    
  /** Produces an RDF VoID dataset description **/
  def getVoID() = DBAction { implicit session =>
    val collections = CollectionMemberships.listCollections()
    
    val voidHeader =
      voidTemplate + 
      Seq.fill[String](collections.size)("  void:subset :collection")
        .zipWithIndex
        .map(t => t._1 + (t._2 + 1) + ";")
        .mkString("\n") +
      "\n  .\n\n"
        
    val subsets = collections.zipWithIndex.map { case (collection, index) => {
      ":collection" + (index + 1) + " a void:dataset;\n" +
      "  dcterms:title \"" + collection + "\";\n" + 
      CollectionMemberships.getGeoDocumentsInCollection(collection).map(id => {
        "  void:dataDump <"  + routes.DownloadController.downloadAnnotationsCSV(id + ".csv").absoluteURL(false) + "?nocoords=true>;"
      }).mkString("\n")
    }}.mkString("\n  .\n\n") + "\n  ."
      
    Ok(voidHeader + subsets)  
  }  
  
  /** Produces a CSV download for the annotations on the specified document **/
  def downloadAnnotationsCSV(gdocId: String) = DBAction { implicit session =>
    val id = 
      try { 
        gdocId match {
          case s if s.endsWith(DOT_CSV) => Some(s.substring(0, s.indexOf(".")).toInt)
          case s => Some(s.toInt)
        }
      } catch {
        case t: Throwable => None
      }
    
    if (id.isDefined) {
      val doc = GeoDocuments.findById(id.get)
      if (doc.isDefined) {    
        val annotations = Annotations.findByGeoDocumentAndStatus(id.get, 
          AnnotationStatus.VERIFIED, 
          AnnotationStatus.NOT_VERIFIED,
          AnnotationStatus.AMBIGUOUS,
          AnnotationStatus.NO_SUITABLE_MATCH,
          AnnotationStatus.MULTIPLE,
          AnnotationStatus.NOT_IDENTIFYABLE)
      
        val excludeCoordinates = session.request.queryString
          .filter(_._1.toLowerCase.equals("nocoords"))
          .headOption.flatMap(_._2.headOption.map(_.toBoolean)).getOrElse(false)
      
        val serializer = new CSVSerializer()
    
        Ok(serializer.serializeAnnotationsConsolidated(doc.get, annotations, !excludeCoordinates))
          .withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=" + escapeTitle(doc.get.title) + doc.get.language.map("_" + _).getOrElse("") + ".csv"))
      } else {
        NotFound
      }
    } else {
      NotFound
    }
  }  
  
  /** Produces an 'annotated text' download.
    * 
    * At the moment, this is only supported for tabular text. The result will
    * be the original CSV that was uploaded, plus an extra 'close match' column
    * with the gazetteer URI.  
    */
  def downloadAnnotatedText(textId: Int) = DBAction { implicit session =>
    val text = GeoDocumentTexts.findById(textId)
    if (text.isDefined) {
      val gdoc = GeoDocuments.findById(text.get.gdocId)
      val annotations = Annotations.findByGeoDocument(text.get.gdocId)
      
      val csv = new TextSerializer().serialize(gdoc.get, text.get, annotations)
      Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=" + escapeTitle(gdoc.get.title) + ".csv"))
    } else {
      NotFound
    }    
  }
  
}
