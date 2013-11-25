package controllers

import play.api.mvc.{ Action, Controller }
import org.pelagios.grct.Global
import play.api.libs.json.Json

/** Toponym search API controller **/
object Search extends Controller {
    
  def index(query: String) = Action {
    val results = Global.index.query(query).map(place => Json.obj(
      "uri" -> place.uri,
      "title" -> place.title,
      "names" -> place.names.map(_.labels).flatten.map(_.label).mkString(", "),
      "coords" -> place.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x)))
    ))
    
    Ok(Json.obj(
      "query" -> query,
      "results" -> Json.toJson(results))
    )
  }

}