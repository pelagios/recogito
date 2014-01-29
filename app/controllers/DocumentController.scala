package controllers

import controllers.io.{ CSVSerializer, JSONSerializer }
import models._
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.{ Json, JsObject }
import play.api.Play.current

/** GeoDocument JSON API.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>  
  */
object DocumentController extends Controller with Secured {
  
  private val CSV = "csv"
  
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
    if (format.isDefined && format.get.equalsIgnoreCase(CSV))
      get_CSV(id)
    else
      get_JSON(id)
  }
  
  private def get_CSV(id: Int)(implicit session: Session) = {
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {
      val annotations = Annotations.findByGeoDocumentAndStatus(id, AnnotationStatus.VERIFIED)
      val serializer = new CSVSerializer()
      Ok(serializer.asConsolidatedResult(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd-" + id.toString + ".csv"))
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))      
    }
       
  }
      
  private def get_JSON(id: Int)(implicit s: Session) = {
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {      
      Ok(JSONSerializer.toJson(doc.get, true))
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))
    }
  }
  
  /** Deletes a document (and associated data) from the database **/
  def delete(id: Int) = protectedDBAction(Secure.REJECT) { username => implicit session =>
    Annotations.deleteForGeoDocument(id)
    GeoDocumentTexts.deleteForGeoDocument(id)
    GeoDocumentParts.deleteForGeoDocument(id)    
    GeoDocuments.delete(id)
    Status(200)
  }
  
}