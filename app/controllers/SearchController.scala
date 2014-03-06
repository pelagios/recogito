package controllers

import global.Global
import play.api.db.slick._
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import play.api.Play.current
import models.{ Annotation, Annotations, AnnotationStatus }

/** Toponym search API controller.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object SearchController extends Controller {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
    
  def placeSearch(query: String) = Action {
    // A little hard-wired hack for Pleiades and DARE:
    // We don't want DARE to appear in the results. BUT:
    //  (i)  we want to use DARE coordinates instead of Pleiades if available
    //  (ii) we want to use DARE names in addition to Pleiades
    val results = Global.index.query(query, true).filter(!_.uri.startsWith(DARE_PREFIX)).map(place => { 
      val (coordinate, names) = {
        val dareEquivalent = Global.index.getNetwork(place).places.filter(_.uri.startsWith(DARE_PREFIX))
        if (dareEquivalent.size > 0) {
          (dareEquivalent(0).getCentroid, place.names ++ dareEquivalent(0).names)
        } else {
          (place.getCentroid, place.names)
        }
      }
      
      Json.obj(
        "uri" -> place.uri,
        "title" -> place.title,
        "names" -> names.map(_.labels).flatten.map(_.label).mkString(", "),
        "category" -> place.category.map(_.toString),
        "coordinate" -> coordinate.map(coords => Json.toJson(Seq(coords.y, coords.x)))
    )})
    
    Ok(Json.obj(
      "query" -> query,     "results" -> Json.toJson(results))
    )
  }

}