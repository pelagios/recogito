package models.stats

import models._
import play.api.db.slick._

trait GeoDocumentPartStats {
  
  val id: Option[Int]
  
  /** Returns the ratio of manually processed vs. total toponyms in the document part **/
  def completionRatio()(implicit s: Session): Double = {
    val valid = Annotations.findByGeoDocumentPart(id.get).filter(_.status != AnnotationStatus.FALSE_DETECTION)
    val unprocessed = valid.filter(_.status == AnnotationStatus.NOT_VERIFIED)
    (valid.size - unprocessed.size).toDouble / valid.size.toDouble
  }

}