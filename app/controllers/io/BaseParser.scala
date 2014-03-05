package controllers.io

import models.GeoDocumentParts
import play.api.db.slick._
import scala.collection.mutable.HashMap

/** A base trait with functionality commonly used across data parsers **/
trait BaseParser {
  
  /** Poor-man's cache for buffering GDocPart title-to-ID mappings **/
  private val partIdCache = HashMap.empty[String, Option[Int]]
  
  /** Helper method that returns the ID for the specified GeoDocument part.
    *
    * Since this operation is typically performed very often by parser, the result is
    * cached to avoid excessive DB accesses.
    * @param docId the ID of the document the part belongs to
    * @param title the part title
    */
  protected def getPartIdForTitle(docId: Int, title: String)(implicit s: Session): Option[Int] = {
    val partId = partIdCache.get(title)
    if (partId.isDefined) {
      partId.get
    } else {
      val id = GeoDocumentParts.findByGeoDocumentAndTitle(docId, title).map(_.id).flatten
      partIdCache.put(title, id)
      id
    }
  }

}