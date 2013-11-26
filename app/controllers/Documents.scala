package controllers

import models._
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.Play.current
import play.api.libs.json.Json
import org.omg.CosNaming.NamingContextPackage.NotFound
import org.pelagios.grct.Global
import play.api.libs.json.JsObject

object Documents extends Controller {
  
  def listAll = DBAction { implicit session =>
    val documents = GeoDocuments.listAll().map(doc => Json.obj(
      "id" -> doc.id,
      "title" -> doc.title
    ))
      
    Ok(Json.toJson(documents))
  }
  
  def getDocument(id: Int) = DBAction { implicit session =>
    val doc = GeoDocuments.findById(id)
    if (doc.isDefined) {
      Ok(joinToJSON(doc.get))
    } else {
      val msg = "No document with ID " + id
      NotFound(Json.obj("error" -> msg))
    }
  }
  
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
  
  private def joinToJSON(document: GeoDocument)(implicit s: Session) = {
    Json.obj(
      "id" -> document.id,
      "title" -> document.title,
      "parts" -> GeoDocumentParts.findByGeoDocument(document.id.get).map(part => Json.obj(
        "title" -> part.title,
        "source" -> part.source,
        "annotations" -> Annotations.findByGeoDocumentPart(part.id.get).map(annotation => Json.obj(
          "toponym" -> annotation.toponym,
          "status" -> annotation.status.toString,
          "place" -> annotation.automatch.map(placeUriToJson(_)),
          "place_fixed" -> annotation.fix.map(placeUriToJson(_))
        ))
      ))
    )
  }

}