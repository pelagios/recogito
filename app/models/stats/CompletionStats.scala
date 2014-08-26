package models.stats

import models.AnnotationStatus

/** Wrapper/utility case class for packaging completion stats **/
case class CompletionStats(stats: Map[AnnotationStatus.Value, Int]) {
  
  import AnnotationStatus._
  
  private def get(status: AnnotationStatus.Value) = stats.get(status).getOrElse(0)
  
  /** Shortcuts for all possible status values **/
  lazy val notVerified = get(NOT_VERIFIED)
  
  lazy val verified = get(VERIFIED)

  lazy val falseDetection = get(FALSE_DETECTION)
  
  lazy val ignore = get(IGNORE)
  
  lazy val noSuitableMatch = get(NO_SUITABLE_MATCH)
  
  lazy val ambiguous = get(AMBIGUOUS)
  
  lazy val multiple = get(MULTIPLE)
  
  lazy val notIdentifiable = get(NOT_IDENTIFYABLE)
  
  /** All different 'unidentified' status types lumped together **/
  lazy val yellow = noSuitableMatch + ambiguous + multiple + notIdentifiable
  
  /** All - except 'false detection' and 'ignore' **/
  lazy val total = verified + yellow + notVerified
  
  lazy val percentVerified = verified.toDouble / total
  
  lazy val percentYellow = yellow.toDouble / total
  
  lazy val percentComplete = percentVerified + percentYellow
  
  lazy val percentGeoResolved = verified.toDouble / (verified + yellow)

}

object CompletionStats {
  
  def empty = CompletionStats(Map.empty[AnnotationStatus.Value, Int])
  
}