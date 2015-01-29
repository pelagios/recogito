package controllers.api.writer

import models.{ Annotation, Annotations, AnnotationStatus, EditEvent }
import java.sql.Timestamp
import java.util.Date
import play.api.db.slick._
import play.api.libs.json.{ JsArray, JsObject }
import controllers.common.io.JSONSerializer

private[api] trait BaseAnnotationWriter {
  
  /** Deletes an annotation.
    *  
    * Note that an annotation deletion is a bit of complex issue. If we're dealing with a manually created annotation, 
    * we just delete it. Period. BUT: if we deal with an annotation that was created automatically, we still want to keep
    * it in the DB for the purposes of precision/recall estimation. In this case, we therefore don't delete the
    * annotation, but just mark it as a 'false detection'. If the annotation was manually modified, we also remove those
    * manual modifications to restore the original NER state. 
    * @param a the annotation
    */  
  protected def deleteAnnotation(a: Annotation)(implicit s: Session): Option[Annotation] = {
    if (a.toponym.isEmpty) {
      Annotations.delete(a.uuid)
      None
    } else {
      val updated = Annotation(a.uuid, a.gdocId, a.gdocPartId,
                               AnnotationStatus.FALSE_DETECTION, 
                               a.toponym, a.offset, a.anchor, a.gazetteerURI,
                               None, None, None, None, None, None, None, None)
                                 
      Annotations.update(updated)    
      Some(updated)
    }
  }
  
  /** Private helper method that creates an update diff event by comparing original and updated annotation.
    * 
    * @param before the original annotation
    * @param after the updated annotation
    * @param userId the user who made the update
    */
  protected def createDiffEvent(before: Annotation, after: Annotation, username: String)(implicit s: Session): EditEvent = {    
    val updatedStatus = if (before.status.equals(after.status)) None else Some(after.status)
    val updatedToponym = if (before.correctedToponym.equals(after.correctedToponym)) None else after.correctedToponym
    val updatedOffset = if (before.correctedOffset.equals(after.correctedOffset)) None else after.correctedOffset
    val updatedURI = if (before.correctedGazetteerURI.equals(after.correctedGazetteerURI)) None else after.correctedGazetteerURI
    val updatedTags = if (before.tags.equals(after.tags)) None else after.tags
    val updateComment = if (before.comment.equals(after.comment)) None else after.comment
    
    EditEvent(None, before.uuid, username, new Timestamp(new Date().getTime), Some(new JSONSerializer().toJson(before, false, false, false).toString),
              updatedToponym, updatedStatus, updatedURI, updatedTags, updateComment)
  }
  
}
