package controllers

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import models.GeoDocumentTexts
import play.api.Logger

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchController extends Controller {
  
  private val UTF8 = "UTF-8"
  
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
    
  def placeSearch(query: String) = Action {
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
      "query" -> query,     "results" -> Json.toJson(results))
    )
  }
  
  def textSearch(textId: Int, query: String) = DBAction { implicit session =>
    val geoDocText = GeoDocumentTexts.findById(textId)
    if (geoDocText.isDefined) {      
      def next(text: String, occurrences: Seq[Int] = Seq.empty[Int]): Seq[Int] = {
        val idx = text.indexOf(query.toLowerCase)
        if (idx > -1) {
          idx +: next(text.substring(idx + query.size))
        } else {
          occurrences
        }
      }
      
      val occurrences = next(new String(geoDocText.get.text, UTF8).toLowerCase)
      Ok(Json.toJson(occurrences))    
    } else {
      NotFound
    }
  }

}