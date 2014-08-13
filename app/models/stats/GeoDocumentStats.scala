package models.stats

import models.Annotations
import play.api.db.slick._
import models.AnnotationStatus
import play.api.Logger
import models.GeoDocumentParts

/** A helper trait that provides basic stats & metrics for a GeoDocument.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
trait GeoDocumentStats {
  
  val id: Option[Int]
  
  /** # of annotations in the document **/
  def countAnnotations()(implicit s: Session): Int =
    Annotations.countForGeoDocument(id.get)
   
  /** # of parts the document consists of **/
  def countParts()(implicit s: Session): Int =
    GeoDocumentParts.countForGeoDocument(id.get)
  
  /** A quality metric for the automatic geo-resolution.
    *
    * The metric represents the percentage of verified annotations that have either no human correction,
    * or a correction identical to the original gazetteer mapping. 
    */
  def resolutionCorrectness()(implicit s: Session) = {
    val all = Annotations.findByGeoDocumentAndStatus(id.get, AnnotationStatus.VERIFIED)
    val correct = all.filter(a => a.gazetteerURI.isDefined && (
        a.correctedGazetteerURI.isEmpty || 
        a.correctedGazetteerURI.get.trim.isEmpty ||
        a.correctedGazetteerURI == a.gazetteerURI))
        
    correct.size.toDouble / all.size.toDouble
  }
  
  /*
  def uniqueTags(annotations: Iterable[Annotation]): Seq[String] = {
    val uniqueCombinations = annotations.groupBy(_.tags).keys.filter(_.isDefined).map(_.get).toSeq
    uniqueCombinations.map(_.split(",")).flatten.toSet.toSeq
  }
  */
  
}