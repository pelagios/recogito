package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import org.pelagios.grct.Global

/** Toponym search API controller **/
object SearchController extends Controller {
  
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
    
  def index(query: String) = Action {
    // For search, we're restricting to Pleiades URIs only
    val results = Global.index.query(query, true).filter(_.uri.startsWith(PLEIADES_PREFIX)).map(place => { 
      // We use DARE coordinates if we have them
      val coordinate = place.map(place => {
        val dareEquivalent = Global.index.getNetwork(place).places.filter(_.uri.startsWith(DARE_PREFIX))
        if (dareEquivalent.size > 0) {
          dareEquivalent(0).getCentroid
        } else {
          place.getCentroid
        }
      }).flatten      
      
      Json.obj(
        "uri" -> place.uri,
        "title" -> place.title,
        "names" -> place.names.map(_.labels).flatten.map(_.label).mkString(", "),
        "coordinate" -> coordinate.map(coords => Json.toJson(Seq(coords.y, coords.x)))
    )})
    
    Ok(Json.obj(
      "query" -> query,      "results" -> Json.toJson(results))
    )
  }

}