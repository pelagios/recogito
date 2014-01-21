package models.stats

import models.Annotations
import play.api.db.slick._
import models.AnnotationStatus
import play.api.Logger

/**
 * TODO lots of room for optimization (memoization?)
 */
trait GeoDocumentStats {
  
  val id: Option[Int]

  /** Returns the total # of toponyms in the document, i.e. all that were not marked as 'false detection' **/
  def totalToponymCount()(implicit s: Session): Int = 
    Annotations.findByGeoDocument(id.get).filter(a => a.status != AnnotationStatus.FALSE_DETECTION && a.status != AnnotationStatus.IGNORE).size
    
  /** Returns the # of verified annotations in the document **/
  def unverifiedToponymCount()(implicit s: Session): Int =
    Annotations.findByGeoDocumentAndStatus(id.get, AnnotationStatus.NOT_VERIFIED).size
  
  /** Returns the ratio of manually processed vs. total toponyms in the document **/
  def completionRatio()(implicit s: Session): Double = {
    val valid = Annotations.findByGeoDocument(id.get).filter(_.status != AnnotationStatus.FALSE_DETECTION)
    val unprocessed = valid.filter(_.status == AnnotationStatus.NOT_VERIFIED)
    (valid.size - unprocessed.size).toDouble / valid.size.toDouble
  }
  
  /** Returns the ratio of verified vs. unidentifyable places in the document **/
  def identificiationRatio()(implicit s: Session): Double = {
    val verified = Annotations.findByGeoDocumentAndStatus(id.get, AnnotationStatus.VERIFIED).size
    val unidentifyable = Annotations.findByGeoDocumentAndStatus(id.get, AnnotationStatus.NOT_IDENTIFYABLE).size
    verified.toDouble / (verified.toDouble + unidentifyable.toDouble)
  }
  
  /** Returns the NER recall metric for the document.
    *
    * Recall is the total number of toponyms the NER found (including those marked as false 
    * matches) vs. the total number toponyms contained in the document (as reported by human users)  
    */ 
  def nerRecall()(implicit s: Session) = {
    val valid = Annotations.findByGeoDocument(id.get).filter(_.status != AnnotationStatus.FALSE_DETECTION)   
    val ner = valid.filter(a => a.toponym.isDefined && a.correctedToponym.isEmpty)
    ner.size.toDouble / valid.size.toDouble
  }
  
  def nerPrecision()(implicit s: Session) = {
    val allNER = Annotations.findByGeoDocument(id.get).filter(_.toponym.isDefined)
    val validNER = allNER.filter(a => a.status != AnnotationStatus.FALSE_DETECTION && a.correctedToponym.isEmpty)
    validNER.size.toDouble / allNER.size.toDouble
  }
  
  def resolutionCorrectness()(implicit s: Session) = {
    // Sort of dirty - but we only count the verified anntations for now
    val all = Annotations.findByGeoDocument(id.get).filter(a => a.status != AnnotationStatus.FALSE_DETECTION && a.status != AnnotationStatus.IGNORE)
    val correct = all.filter(a => a.gazetteerURI.isDefined && (a.correctedGazetteerURI.isEmpty || a.correctedGazetteerURI.get.trim.isEmpty))
    correct.size.toDouble / all.size.toDouble
  }
  
}