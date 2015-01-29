package controllers.admin

import controllers.common.auth.Secured
import models.{ Annotations, CollectionMemberships }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.mvc.Controller

object DataDumpsController extends Controller with Secured {
  
  private val SEPARATOR = ";"
  
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.datadumps(CollectionMemberships.listCollections))
  }
  
  def annotatedPlaces(collection: String) = adminAction { username => implicit session =>
    val docIds = CollectionMemberships.getGeoDocumentsInCollection(collection)
    val placeStats = Annotations.getPlaceStats(docIds)
    
    val header = Seq("uri", "lon", "lat", "name_in_gazetteer", "number_of_annotations").mkString(SEPARATOR) + "\n"
    val csv = placeStats.uniquePlaces.map { case (place, count, _) =>
      val coord = place.getCentroid
      Seq(
        place.uri,
        coord.map(_.x.toString).getOrElse(""),
        coord.map(_.y.toString).getOrElse(""),
        place.label,
        count.toString
      ).mkString(SEPARATOR)
    }
    
    val filename = "annotated-places-" + collection.toLowerCase().replace(" ", "-") + ".csv"
    Ok(header + csv.mkString("\n")).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> { "attachment; filename=" + filename })
  }

}