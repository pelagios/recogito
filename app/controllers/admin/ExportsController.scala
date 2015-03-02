package controllers.admin

import controllers.common.auth.Secured
import models.{ Annotations, CollectionMemberships }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.mvc.Controller
import controllers.common.io.CSVSerializer

object ExportsController extends Controller with Secured {
  
  private val SEPARATOR = ";"
  
  def index = adminAction { username => implicit session =>
    Ok(views.html.admin.exports(CollectionMemberships.listCollections))
  }
  
  def placeStats(collection: String) = adminAction { username => implicit session =>
    val docIds = CollectionMemberships.getGeoDocumentsInCollection(collection)
    val filename = "annotated-places-" + collection.toLowerCase().replace(" ", "-") + ".csv"
    val csv = new CSVSerializer().serializePlaceStats(Annotations.getPlaceStats(docIds))
    Ok(csv).withHeaders(CONTENT_TYPE -> "text/csv", CONTENT_DISPOSITION -> { "attachment; filename=" + filename })
  }

}