package controllers

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import models.{ Annotation, Annotations, AnnotationStatus }
import org.pelagios.gazetteer.PlaceDocument
import org.pelagios.gazetteer.Network
import org.pelagios.api.Place

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchController extends Controller {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
    
  private def conflateNetwork(network: Network) = {
    if (network.places.size == 1) {
      val place = network.places.head
      Json.obj(
        "uri" -> place.uri,
        "title" -> place.title,
        "names" -> place.names.map(_.label).mkString(", "),
        "description" -> place.descriptions.map(_.label).mkString(", "),
        "category" -> place.category.map(_.toString),
        "coordinate" -> place.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x))))
    } else {
      val head = network.places.head
      val tail = network.places.tail
      
      // We prefer DARE coordinates (if we have them) because of our background map
      val coordinate = {
        val dare = network.places.filter(_.uri.startsWith(DARE_PREFIX))
        if (dare.size > 0)
          dare.head.getCentroid
        else
          head.getCentroid
      }
      
      // We prefer Pleiades URI and description (if we have it)
      val (uri, descriptions) = {
        val pleiades = network.places.filter(_.uri.startsWith(PLEIADES_PREFIX))
        if (pleiades.size > 0)
          (pleiades.head.uri, pleiades.head.descriptions)
        else
          (head.uri, head.descriptions) 
      }
      
      Json.obj(
        "uri" -> uri,
        "title" -> head.title,
        "names" -> (head.names ++ tail.flatMap(_.names)).map(_.label).mkString(", "),
        "description" -> descriptions.map(_.label).mkString(", "),
        "category" -> head.category.map(_.toString),
        "coordinate" -> coordinate.map(coords => Json.toJson(Seq(coords.y, coords.x))))
    }
  }
    
  def placeSearch(query: String) = Action {
    val networks = Global.index.query(query, true).map(Global.index.getNetwork(_)).toSet    
    val results = networks.map(conflateNetwork(_))
    Ok(Json.obj("query" -> query, "results" -> Json.toJson(results)))
  }

}
