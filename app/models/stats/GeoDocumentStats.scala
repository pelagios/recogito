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
  
  /** Total # of toponyms in the document, i.e. all that were not marked as 'false detection' or 'ignore' **/
  def countTotalToponyms()(implicit s: Session): Int =
    Annotations.countForGeoDocumentAndStatus(id.get, 
      AnnotationStatus.VERIFIED, 
      AnnotationStatus.NOT_VERIFIED, 
      AnnotationStatus.AMBIGUOUS,
      AnnotationStatus.NO_SUITABLE_MATCH,
      AnnotationStatus.MULTIPLE,
      AnnotationStatus.NOT_IDENTIFYABLE)
    
  /** # of verified (i.e. 'green') annotations in the document **/
  def countVerifiedToponyms()(implicit s: Session): Int =
    Annotations.countForGeoDocumentAndStatus(id.get, AnnotationStatus.VERIFIED)
        
  /** # of unidentifiable (i.e. 'yellow') annotations in the document **/
  def countUnidentifiableToponyms()(implicit s: Session): Int =
    Annotations.countForGeoDocumentAndStatus(id.get,
      AnnotationStatus.AMBIGUOUS,
      AnnotationStatus.NO_SUITABLE_MATCH,
      AnnotationStatus.MULTIPLE,
      AnnotationStatus.NOT_IDENTIFYABLE)
    
  /** # of unverified annotations in the document **/
  def countUnverifiedToponyms()(implicit s: Session): Int =
    Annotations.countForGeoDocumentAndStatus(id.get, AnnotationStatus.NOT_VERIFIED)
  
  /** Ratio of manually processed vs. total toponyms in the document **/
  def completionRatio()(implicit s: Session): Double = {
    val valid = Annotations.findByGeoDocument(id.get).filter(_.status != AnnotationStatus.FALSE_DETECTION)
    val unprocessed = valid.filter(_.status == AnnotationStatus.NOT_VERIFIED)
    (valid.size - unprocessed.size).toDouble / valid.size.toDouble
  }
  
  /** Ratio of verified vs. unidentifyable places in the document **/
  def identificiationRatio()(implicit s: Session): Double = {
    val verified = Annotations.countForGeoDocumentAndStatus(id.get, AnnotationStatus.VERIFIED)
    val unidentifyable = Annotations.countForGeoDocumentAndStatus(id.get, AnnotationStatus.NOT_IDENTIFYABLE)
    verified.toDouble / (verified.toDouble + unidentifyable.toDouble)
  }
  
  /** NER recall for the document.
    *
    * Recall is the total number of toponyms the NER found (including false detections) vs. 
    * the total number toponyms contained in the document (as reported by human users)  
    */ 
  def nerRecall()(implicit s: Session) = {
    val valid = Annotations.findByGeoDocument(id.get).filter(_.status != AnnotationStatus.FALSE_DETECTION)   
    val ner = valid.filter(a => a.toponym.isDefined && a.correctedToponym.isEmpty)
    ner.size.toDouble / valid.size.toDouble
  }
  
  /** NER precision for the document.
    * 
    * Precision is the number of all toponyms the NER found (including false detections) vs.
    * the number of NER-identified toponyms that were accepted as valid toponyms by human users.
    */
  def nerPrecision()(implicit s: Session) = {
    val allNER = Annotations.findByGeoDocument(id.get).filter(_.toponym.isDefined)
    val validNER = allNER.filter(a => a.status != AnnotationStatus.FALSE_DETECTION)
    validNER.size.toDouble / allNER.size.toDouble
  }
  
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
  
}