package controllers

import models._
import org.pelagios.grct.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.json.JsObject

/** GeoDocument JSON API **/
object Documents extends Controller {
  
  /** Returns the list of all geo documents in the database as JSON **/
  def listAll = DBAction { implicit session =>
    val documents = GeoDocuments.listAll().map(doc => Json.obj(
      "id" -> doc.id,
      "title" -> doc.title
    ))
      
    Ok(Json.toJson(documents))
  }
  
  /** Returns the JSON data for a specific geo document **/
  def getDocument(id: Int) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {
      Ok(joinToJSON(doc.get))
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))
    }
  }
  
  /** Renders a JSON object for the place with the specified gazetteer URI **/
  private def placeUriToJson(uri: String): Option[JsObject] = {
    val place = Global.index.getPlace(uri)
    if (place.isDefined) {
      Some(Json.obj(
        "uri" -> place.get.uri,
        "title" -> place.get.title,
        "names" -> place.get.names.map(_.labels).flatten.map(_.label).mkString(", "),
        "coordinate" -> place.get.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x)))
      ))      
    } else {
      None
    }
  }
  
  /** Renders a JSON serialization for the entire document.
    *  
    * The method also joins the information from the {{GeoDocumentParts}} and
    * {{Annotations}} tables.
    */
  private def joinToJSON(document: GeoDocument)(implicit s: Session) = {
    Json.obj(
      "id" -> document.id,
      "title" -> document.title,
      "parts" -> GeoDocumentParts.findByGeoDocument(document.id.get).map(part => Json.obj(
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
    )
  }

}