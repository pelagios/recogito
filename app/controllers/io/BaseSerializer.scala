package controllers.io

import models._
import play.api.db.slick._
import scala.collection.mutable.HashMap

/** A base trait with functionality commonly used across data serializers **/
trait BaseSerializer {
  
  /** Poor-man's cache for buffering GDocID-to-GDoc mappings 
    * 
    * NOTE: the 'cache' is just a HashMap. But we assume that a new serializer is 
    * instantiated for (and disposed after) each serialization, so we're fine. Global
    * re-use of a serializer instance would potentially result in a memory leak.  
    */
  private val docCache = HashMap.empty[Int, Option[GeoDocument]]
  
  /** Poor-man's cache for buffering GDocPartID-to-GDocPart mappings **/
  private val partCache = HashMap.empty[Int, Option[GeoDocumentPart]]

  /** Helper method that returns the GeoDocument with the specified ID.
    *
    * Since this operation is typically performed often in serialization scenarios, 
    * the result is cached to avoid excessive DB accesses.
    * @param docId the ID of the document to fetch
    */
  protected def getDocument(docId: Int)(implicit s: Session): Option[GeoDocument] = {
    val cachedDoc = docCache.get(docId)
    if (cachedDoc.isDefined) {
      cachedDoc.get
    } else {
      val doc = GeoDocuments.findById(docId)
      docCache.put(docId, doc)
      doc
    }        
  }
  
  /** Helper method that returns the GeoDocumentPart with the specified ID.
    *
    * Since this method is typically performed often in serialization scenarios,
    * the result is cached to avoid excessive DB access.
    * @param partId the ID of the document part to fetch
    */
  protected def getPart(partId: Int)(implicit s: Session): Option[GeoDocumentPart] = {
    val cachedPart = partCache.get(partId)
    if (cachedPart.isDefined) {
      cachedPart.get
    } else {
      val part = GeoDocumentParts.findById(partId)
      partCache.put(partId, part)
      part
    }    
  }
  
  /** Helper method that determines the source for the annotation. 
    *  
    * An annotation may have a explicit source defined in its 'source' field. If
    * not, this method traverses up the hierarchy, first checking for the source
    * of the associated GeoDocumentPart, and then of the associated GeoDocument.
    * @param annotation the annotation
    */
  protected def getSourceForAnnotation(annotation: Annotation)(implicit s: Session): Option[String] = {
    if (annotation.source.isDefined) {
      annotation.source
    } else {
      val part = annotation.gdocPartId.map(getPart(_)).flatten
      val partSource = part.map(_.source).flatten
      if (partSource.isDefined) {
        partSource
      } else {
        annotation.gdocId.map(id => getDocument(id)).flatten.map(_.source).flatten
      }
    }    
  }
  
}