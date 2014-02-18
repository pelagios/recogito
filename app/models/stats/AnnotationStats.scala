package models.stats

import global.Global
import models.Annotation
import org.pelagios.api.{ Place, PlaceCategory }
import org.pelagios.gazetteer.GazetteerUtils
import play.api.Logger

object AnnotationStats {
  
  def uniqueTags(annotations: Iterable[Annotation]): Seq[String] = {
    val uniqueCombinations = annotations.groupBy(_.tags).keys.filter(_.isDefined).map(_.get).toSeq
    uniqueCombinations.map(_.split(",")).flatten.toSet.toSeq
  }
  
  def uniquePlaces(annotations: Iterable[Annotation]): Seq[(Place, Int)] = {
    val uniqueURIs = annotations.groupBy(_.validGazetteerURI.map(GazetteerUtils.normalizeURI(_))) // Group by (normalized!) valid URI
      .filter { case (uri, annotations) => uri.isDefined && uri.get.trim.size > 0 } // Filter empty URIs
      .map { case (uri, annotations) => (uri.get, annotations.size) }.toSeq // Map to (uri -> no. of occurrences)
      
    // Map from (uri -> occurrences) to (place -> occurrences)
    val uniquePlaces = uniqueURIs.map(tuple => (tuple._1, Global.index.findByURI(tuple._1), tuple._2))
    
    val integrityViolations = uniquePlaces.filter(_._2.isEmpty)
    integrityViolations.foreach(violation => {
      Logger.warn("Annotation contains invalid URI: " + violation._1)
    })
    
    uniquePlaces.map(tuple => (tuple._2.get, tuple._3)) // We should never have any undefined URIs in practice - if we do: fail early, fail often!
      .sortBy(t => (-t._2, t._1.title))
  }
  
  def uniquePlaceCategories(annotations: Iterable[Annotation]): Seq[(Option[PlaceCategory.Category], Int)] =
    uniquePlaces(annotations).groupBy(_._1.category).mapValues(places => places.foldLeft(0)(_ + _._2)).toSeq.sortBy(_._2).reverse

}