package models

import scala.slick.lifted.MappedTypeMapper

/** Possible annotation status values.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AnnotationStatus extends Enumeration {
  
  val NOT_VERIFIED = Value("NOT_VERIFIED")
  
  val VERIFIED = Value("VERIFIED")
  
  val NOT_IDENTIFYABLE = Value("NOT_IDENTIFYABLE")
  
  val FALSE_DETECTION = Value ("FALSE_DETECTION")
  
  val IGNORE = Value("IGNORE")
  
}

trait HasStatusColumn {
  
  implicit val statusMapper = MappedTypeMapper.base[AnnotationStatus.Value, String](
    { status => status.toString },
    { status => AnnotationStatus.withName(status) })
    
}
