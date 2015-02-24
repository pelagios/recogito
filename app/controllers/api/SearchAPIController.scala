package controllers.api

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import play.api.Logger
import net.sf.junidecode.Junidecode
import global.CrossGazetteerUtils
import index.IndexedPlace

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchAPIController extends Controller {
  
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
    
  def placeSearch(query: String) = Action { implicit request =>
    val gazetteerPrefixes = request.queryString
          .filter(_._1.toLowerCase.equals("prefix"))
          .headOption.flatMap(_._2.headOption)
          .map(_.split(",").toSeq.map(_.trim))
              
    val networks = Global.index.search(query, 100, 0, gazetteerPrefixes)
    val conflatedPlaces = networks.map(network => {
      val places = 
        if (gazetteerPrefixes.isDefined) {
          network.places.filter(place => gazetteerPrefixes.get.exists(place.uri.startsWith(_)))
        } else {
          network.places
        }
      
      val preferredPlace = // Hard-wired hack :-( Pleiades preferred by convention
        places.filter(_.uri.startsWith(PLEIADES_PREFIX))
              .headOption
              .getOrElse(places.head)
      
      val preferredGeometry =
        CrossGazetteerUtils.getPreferredGeometry(preferredPlace, network)
        
      (preferredPlace, preferredGeometry)
    })

    Ok(Json.obj("query" -> query, "results" -> conflatedPlaces.map { case (place, geometry) => {
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
          "description" -> place.description,
          "category" -> place.category.map(_.toString),
          "geometry" -> geometry.map(IndexedPlace.geometry2geoJSON(_)),
          "coordinate" -> geometry.map(g => { 
            val coords = g.getCentroid.getCoordinate
            Json.toJson(Seq(coords.y, coords.x)) 
          })
        ) 
    }}))
  }

}
