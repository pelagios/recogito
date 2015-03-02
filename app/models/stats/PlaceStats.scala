package models.stats

import index.{ IndexedPlace, IndexedPlaceNetwork }
import org.pelagios.api.gazetteer.PlaceCategory

/** Wrapper/utility case class for packaging place & toponym statistics **/
case class PlaceStats(uniquePlaces: Seq[(IndexedPlace, IndexedPlaceNetwork, Int, Seq[(String, Int)])]) {

  lazy val uniquePlaceCategories: Seq[(Option[PlaceCategory.Category], Int)] =
    uniquePlaces.groupBy(_._1.category).mapValues(places => places.foldLeft(0)(_ + _._3)).toSeq.sortBy(- _._2)
  
}
