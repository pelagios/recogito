package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import org.pelagios.grct.Global

/** Toponym search API controller **/
object SearchController extends Controller {
  
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
    
  def index(query: String) = Action {
    // For search, we're restricting to Pleiades URIs only
    val results = Global.index.query(query).filter(_.uri.startsWith(PLEIADES_PREFIX)).map(place => Json.obj(
      "uri" -> place.uri,
      "title" -> place.title,
      "names" -> place.names.map(_.labels).flatten.map(_.label).mkString(", "),
      "coordinate" -> place.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x)))
    ))
    
    Ok(Json.obj(
      "query" -> query,      "results" -> Json.toJson(results))
    )
  }

}