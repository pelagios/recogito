package controllers.unrestricted

import controllers.common.io.CSVSerializer
import models.{ Annotations, AnnotationStatus, CollectionMemberships, GeoDocuments }
import play.api.db.slick._
import play.api.mvc.Controller
import scala.io.Source

object DownloadController extends Controller {
  
  private val DOT_CSV = ".csv"
  
  private val voidTemplate =
    Source.fromFile("conf/void-template.ttl").getLines().takeWhile(!_.trim.startsWith(".")).mkString("\n") + "\n"

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
        "  void:dataDump <"  + routes.DownloadController.downloadCSV(id + ".csv").absoluteURL(false) + "?nocoords=true>;"
      }).mkString("\n")
    }}.mkString("\n  .\n\n") + "\n  ."
      
    Ok(voidHeader + subsets)  
  }  
  
  def downloadCSV(gdocId: String) = DBAction { implicit session =>
    // TODO parse gdocId (allow with or without .csv) and fetch doc from DB
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
          AnnotationStatus.AMBIGUOUS,
          AnnotationStatus.NO_SUITABLE_MATCH,
          AnnotationStatus.MULTIPLE,
          AnnotationStatus.NOT_IDENTIFYABLE)
      
        val excludeCoordinates = session.request.queryString
          .filter(_._1.toLowerCase.equals("nocoords"))
          .headOption.flatMap(_._2.headOption.map(_.toBoolean)).getOrElse(false)
      
        val serializer = new CSVSerializer()
    
        def escapeTitle(title: String) = 
          title.replace(" ", "_").replace(",", "_")
    
        Ok(serializer.serializeAnnotationsConsolidated(doc.get, annotations, !excludeCoordinates))
          .withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=" + escapeTitle(doc.get.title) + doc.get.language.map("_" + _).getOrElse("") + ".csv"))
      } else {
        NotFound
      }
    } else {
      NotFound
    }
  }  
  
}
