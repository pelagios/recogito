package controllers.io

import global.{ Global, CrossGazetteerUtils }
import models._
import org.pelagios.gazetteer.GazetteerUtils
import play.api.db.slick._
import scala.collection.mutable.HashMap

/** Utility object to serialize Annotation data to CSV.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class CSVSerializer {
  
  private val SEPARATOR = ";"
    
  private val docCache = HashMap.empty[Int, Option[GeoDocument]]
  private val partCache = HashMap.empty[Int, Option[GeoDocumentPart]]
  
  /** Helper method that returns the GeoDocumentPart with the specified ID.
    *
    * Since this method may be called multiple times for each annotation that is 
    * exported, the result is cached to avoid excessive DB access.
    * 
    * NOTE: the 'cache' is just a HashMap. But we assume that a new CSVSerializer is 
    * instantiated for (and disposed after) each CSV export. Global re-use of a CSVSerializer
    * instance would theoretically result in a memory leak.
    */
  private def getPart(partId: Int)(implicit s: Session): Option[GeoDocumentPart] = {
    val cachedPart = partCache.get(partId)
    if (cachedPart.isDefined) {
      cachedPart.get
    } else {
      val part = GeoDocumentParts.findById(partId)
      partCache.put(partId, part)
      part
    }    
  }

  /** Helper method that returns the GeoDocument with the specified ID.
    *
    * Since this method is called for each annotation that is exported, the result
    * is cached to avoid excessive DB access.
    */
  private def getDocument(docId: Int)(implicit s: Session): Option[GeoDocument] = {
    val cachedDoc = docCache.get(docId)
    if (cachedDoc.isDefined) {
      cachedDoc.get
    } else {
      val doc = GeoDocuments.findById(docId)
      docCache.put(docId, doc)
      doc
    }        
  }
  
  /**
   * Helper method that determines the source for the annotation. An annotation may have a
   * explicit source defined in its .source field. If not, this method traverses upwards in the
   * hierarchy, first checking for the source of the associated GeoDocumentPart, and then the
   * associated GeoDocument.
   */
  private def getSourceForAnnotation(annotation: Annotation)(implicit s: Session): Option[String] = {
    if (annotation.source.isDefined) {
      annotation.source
    } else {
      val part = annotation.gdocPartId.map(getPart(_)).flatten
      val partSource = part.map(_.source).flatten
      if (partSource.isDefined) {
        partSource
      } else {
        getDocument(annotation.gdocId).map(_.source).flatten
      }
    }    
  }
  
  /** Generates 'consolidated output' for public consumption.
    *
    * This version of the CSV data exposes *only* the verified annotations,
    * with just the final, corrected toponyms and gazetteer IDs. Information about
    * whether data was generated automatically or manually, what corrections
    * were made, etc. is no longer included in this data. CSV files of this type 
    * CANNOT be used to restore data in the DB!
    * @param annotations the annotations
    * @return the CSV
    */
  def asConsolidatedResult(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("toponym","uri","lat","lng", "place_category", "tags", "source").mkString(SEPARATOR) + ";\n"
    annotations.foldLeft(header)((csv, annotation) => {
      val uri = if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty) annotation.correctedGazetteerURI else annotation.gazetteerURI
      val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
      
      if (uri.isDefined && !uri.get.isEmpty) {
        val queryResult = CrossGazetteerUtils.getPlace(uri.get)
        val category = queryResult.map(_._1.category).flatten
        val coord = queryResult.map(_._2).flatten
        
        csv + 
        toponym.getOrElse("") + SEPARATOR + 
        GazetteerUtils.normalizeURI(uri.get) + SEPARATOR + 
        coord.map(_.y).getOrElse("") + SEPARATOR +
        coord.map(_.x).getOrElse("") + SEPARATOR +
        category.map(_.toString).getOrElse("") + SEPARATOR +
        { if (annotation.tags.size > 0) "\"" + annotation.tags.mkString(",") + "\"" else "" } + SEPARATOR + 
        getSourceForAnnotation(annotation).getOrElse("") + SEPARATOR + "\n"
      } else {
        csv
      }
    })
  }
  
  /** Generates a full backup annotations from the database.
    * 
    * This version of the CSV data exposes all original fields from the annotations table
    * in the database. CSV files of this type can be used to restore data in the DB.
    * @param annotations the annotations
    * @return the CSV
    */
  def asDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("uuid", "gdoc_part", "status", "toponym", "offset", "gazetteer_uri", "latlon", "place_category", "toponym_corrected", 
                     "offset_corrected", "gazetteer_uri_corrected", "latlon_corrected", "place_category_corrected", "tags", "comment", "see_also").mkString(SEPARATOR) + ";\n"
      
    annotations.foldLeft(header)((csv, annotation) => {
      val queryResultForURI = annotation.gazetteerURI.map(uri => CrossGazetteerUtils.getPlace(uri)).flatten
      val queryResultForCorrectedURI = annotation.correctedGazetteerURI.map(uri => CrossGazetteerUtils.getPlace(uri)).flatten
          
      val placeCategory = queryResultForURI.map(_._1.category).flatten
      val coordinate = queryResultForURI.map(_._2).flatten
      
      val correctedPlaceCategory = queryResultForCorrectedURI.map(_._1.category).flatten
      val correctedCoordinate = queryResultForCorrectedURI.map(_._2).flatten
      
      csv + 
      annotation.uuid + SEPARATOR +
      annotation.gdocPartId.map(getPart(_).map(_.title)).flatten.getOrElse("") + SEPARATOR +
      annotation.status + SEPARATOR +
      annotation.toponym.getOrElse("") + SEPARATOR +
      annotation.offset.getOrElse("") + SEPARATOR +
      annotation.gazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      coordinate.map(c => c.x + "," + c.y).getOrElse("") + SEPARATOR +
      placeCategory.map(_.toString).getOrElse("") + SEPARATOR +
      annotation.correctedToponym.getOrElse("") + SEPARATOR +
      annotation.correctedOffset.getOrElse("") + SEPARATOR +
      annotation.correctedGazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      correctedCoordinate.map(c => c.x + "," + c.y).getOrElse("") + SEPARATOR +
      correctedPlaceCategory.map(_.toString).getOrElse("") + SEPARATOR +
      annotation.tags.getOrElse("") + SEPARATOR +
      annotation.comment.getOrElse("") + SEPARATOR +
      annotation.seeAlso.mkString(",") + SEPARATOR +
      "\n"
    })
  }

}