package models

import scala.slick.lifted.MappedTypeMapper

/** Possible annotation status values.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AnnotationStatus extends Enumeration {
  
  /** The annotation is not manually verified **/
  val NOT_VERIFIED = Value("NOT_VERIFIED")
  
  /** The annotation is verified and assumed to be correct **/
  val VERIFIED = Value("VERIFIED")  
  
  /** The annotation was generated automatically, but was a false detection **/ 
  val FALSE_DETECTION = Value ("FALSE_DETECTION")
  
  /** Ignore this annotation for the purposes of mapping (for whatever reason) **/
  val IGNORE = Value("IGNORE")
  
  /** There was no suitable match in the gazetteer for this place **/ 
  val NO_SUITABLE_MATCH = Value("NO_SUITABLE_MATCH")
  
  /** There were multiple gazetteer entries this place may correspond to **/
  val AMBIGUOUS = Value("AMBIGUOUS")
  
  /** The annotation/toponym refers to multiple places at once **/
  val MULTIPLE = Value("MULTIPLE")
  
  /** This place is not identifiable (for whatever reason) **/
  val NOT_IDENTIFYABLE = Value("NOT_IDENTIFYABLE")
  
  def screenName(status: AnnotationStatus.Value) = {
    status match {
      case NOT_VERIFIED => "Unverified"
      case VERIFIED => "Verified"
      case FALSE_DETECTION => "False Detection, Deleted"
      case IGNORE => "Ignore"
      case NO_SUITABLE_MATCH => "No Suitable Gazetteer URI"
      case AMBIGUOUS => "Ambiguous - Multiple Possible URIs"
      case MULTIPLE => "Toponym Refers to Multiple Places"
      case NOT_IDENTIFYABLE => "Unknown, Not Identifyable"
    }
  }
  
}

trait HasStatusColumn {
  
  implicit val statusMapper = MappedTypeMapper.base[AnnotationStatus.Value, String](
    { status => status.toString },
    { status => AnnotationStatus.withName(status) })
    
}
