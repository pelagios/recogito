package controllers

import models._
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.{ Json, JsObject }
import play.api.Play.current
import org.pelagios.grct.Global
import org.pelagios.grct.io.CSVSerializer

/** GeoDocument JSON API.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>  
  */
object DocumentController extends Controller {
  
  private val CSV = "csv"
    
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  
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
    val parts = GeoDocumentParts.findByGeoDocument(id)
    val annotations = parts.map(part => Annotations.findByGeoDocumentPart(part.id.get)).flatten
    Ok(CSVSerializer.asConsolidatedVerifiedResult(annotations)).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd-" + id.toString + ".csv"))
  }
      
  private def get_JSON(id: Int)(implicit session: Session) = {
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {      
      Ok(Json.obj("id" -> doc.get.id,
                  "title" -> doc.get.title,
                  "parts" -> GeoDocumentParts.findByGeoDocument(doc.get.id.get).map(part => Json.obj(
                    "title" -> part.title,
                    "source" -> part.source,
                    "annotations" -> Annotations.findByGeoDocumentPart(part.id.get).map(annotation => Json.obj(
                      "id" -> annotation.id,
                      "toponym" -> { if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym },
                      "status" -> annotation.status.toString,
                      "place" -> annotation.gazetteerURI.map(placeUriToJson(_)),
                      "place_fixed" -> annotation.correctedGazetteerURI.map(placeUriToJson(_)),
                      "tags" -> annotation.tags.map(_.split(","))
                    ))
                  ))
                ))
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))
    }
  }
    
  /** Renders a JSON object for the place with the specified gazetteer URI **/
  private def placeUriToJson(uri: String): Option[JsObject] = {
    val place = Global.index.findByURI(uri)
    
    // We use DARE coordinates if we have them
    val coordinate = place.map(place => {
      val dareEquivalent = Global.index.getNetwork(place).places.filter(_.uri.startsWith(DARE_PREFIX))
      if (dareEquivalent.size > 0) {
        dareEquivalent(0).getCentroid
      } else {
        place.getCentroid
      }
    }).flatten
    
    if (place.isDefined) {
      Some(Json.obj(
        "uri" -> place.get.uri,
        "title" -> place.get.title,
        "names" -> place.get.names.map(_.labels).flatten.map(_.label).mkString(", "),
        "coordinate" -> coordinate.map(coords => Json.toJson(Seq(coords.y, coords.x)))
      ))      
    } else {
      None
    }
  }
  
}