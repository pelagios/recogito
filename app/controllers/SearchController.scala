package controllers

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import org.pelagios.gazetteer.Network

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchController extends Controller {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
    
  def placeSearch(query: String) = Action {
    val networks = Global.index.query(query, true).map(Global.index.getNetwork(_))
    val results = Network.conflateNetworks(networks.toSeq, 
        Some(PLEIADES_PREFIX), // prefer Pleiades URIs
        Some(DARE_PREFIX),     // prefer DARE for coordinates
        Some(PLEIADES_PREFIX)) // prefer Pleiades for descriptions
    
    Ok(Json.obj("query" -> query, "results" -> results.map(place => Json.obj(
        "uri" -> place.uri,
        "title" -> place.title,
        "names" -> place.names.map(_.chars).mkString(", "),
        "description" -> place.descriptions.map(_.chars).mkString(", "),
        "category" -> place.category.map(_.toString),
        "coordinate" -> place.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x)))))))
  }

}
