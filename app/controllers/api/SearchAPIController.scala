package controllers.api

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import org.pelagios.gazetteer.Network
import play.api.Logger
import net.sf.junidecode.Junidecode

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchAPIController extends Controller {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
    
  def placeSearch(query: String) = Action {
    val networks = Global.index.query(query).map(Global.index.getNetwork(_))
    val results = Network.conflateNetworks(networks.toSeq, 
        Some(PLEIADES_PREFIX), // prefer Pleiades URIs
        Some(DARE_PREFIX),     // prefer DARE for coordinates
        Some(PLEIADES_PREFIX)) // prefer Pleiades for descriptions
            
    Ok(Json.obj("query" -> query, "results" -> results.map(place => {
      
        val namesEnDeFrEsIt = {
          place.names.filter(_.lang == Some("eng")) ++
          place.names.filter(_.lang == Some("deu")) ++
          place.names.filter(_.lang == Some("fra")) ++
          place.names.filter(_.lang == Some("spa")) ++
          place.names.filter(_.lang == Some("ita"))
        } map (_.chars)
        
        val otherNames = place.names.map(_.chars) diff namesEnDeFrEsIt
        
        Json.obj(
          "uri" -> place.uri,
          "title" -> place.label,
          "names" -> Json.toJson(namesEnDeFrEsIt ++ otherNames), // place.names.map(_.chars)),
          "description" -> place.descriptions.map(_.chars).mkString(", "),
          "category" -> place.category.map(_.toString),
          "geometry" -> place.locations.headOption.map(location => Json.parse(location.geoJSON)),
          "coordinate" -> place.getCentroid.map(coords => Json.toJson(Seq(coords.y, coords.x)))) })))
  }

}
