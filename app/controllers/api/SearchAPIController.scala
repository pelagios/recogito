package controllers.api

import com.vividsolutions.jts.geom.Geometry
import global.{ Global, CrossGazetteerUtils }
import index.IndexedPlace
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import play.api.Logger

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

    val conflatedPlaces = networks.foldLeft(Seq.empty[(IndexedPlace, Option[Geometry])])((result, network) => {
      val places = 
        if (gazetteerPrefixes.isDefined) {
          // If search was on a specific gazetteer, we keep *all* places that satisfy the prefix filter
          network.places.filter(place => gazetteerPrefixes.get.exists(place.uri.startsWith(_)))
        } else {
          // Otherwise, we pick only the first place from the list
          // Caveat: we prefer a Pleiades record, if there is one - hard wired hack :-(
          val pleiadesPlaces = network.places.filter(_.uri.startsWith(PLEIADES_PREFIX))
          if (pleiadesPlaces.size > 0)
            Seq(pleiadesPlaces.head)
          else
            Seq(network.places.head)
        }
      
      val preferredGeometry =
        CrossGazetteerUtils.getPreferredGeometry(places.head, network)
      
      result ++ places.map(place => (place, preferredGeometry))
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
          "names" -> Json.toJson(namesEnDeFrEsIt ++ otherNames),
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
