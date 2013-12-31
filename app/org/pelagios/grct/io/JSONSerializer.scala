package org.pelagios.grct.io

import models._
import org.pelagios.grct.Global
import play.api.db.slick._
import play.api.libs.json.{ Json, JsObject }

object JSONSerializer {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"

  def toJson(a: Annotation, includeContext: Boolean = false): JsObject = {
    Json.obj(
      "id" -> a.id,
      "toponym" -> { if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym },
      "status" -> a.status.toString,
      "place" -> a.gazetteerURI.map(placeUriToJson(_)),
      "place_fixed" -> a.correctedGazetteerURI.map(placeUriToJson(_)),
      "tags" -> a.tags.map(_.split(",")))
  }
  
  def toJson(doc: GeoDocument)(implicit session: Session): JsObject = {
    Json.obj(
      "id" -> doc.id,
      "title" -> doc.title,
      "parts" -> GeoDocumentParts.findByGeoDocument(doc.id.get).map(part => Json.obj(
        "title" -> part.title,
        "source" -> part.source,
        "annotations" -> Annotations.findByGeoDocumentPart(part.id.get).map(toJson(_))
      )
    ))       
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