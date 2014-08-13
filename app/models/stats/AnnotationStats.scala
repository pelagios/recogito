package models.stats

import global.Global
import models.Annotation
import org.pelagios.api.gazetteer.{ Place, PlaceCategory }
import org.pelagios.gazetteer.GazetteerUtils
import play.api.Logger

object OtherStats {
  
  def uniqueTags(annotations: Iterable[Annotation]): Seq[String] = {
    val uniqueCombinations = annotations.groupBy(_.tags).keys.filter(_.isDefined).map(_.get).toSeq
    uniqueCombinations.map(_.split(",")).flatten.toSet.toSeq
  }

}