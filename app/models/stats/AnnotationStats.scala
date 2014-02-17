package models.stats

import models.Annotation

object AnnotationStats {
  
  def uniqueTags(annotations: Iterable[Annotation]): Seq[String] = {
    val uniqueCombinations = annotations.groupBy(_.tags).keys.filter(_.isDefined).map(_.get)
    uniqueCombinations.toSeq.map(_.split(",")).flatten.toSet.toSeq.filter(str => !str.isEmpty)
  }

}