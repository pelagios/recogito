package controllers.common.io

import controllers.common.ImageAnchor
import global.{ Global, CrossGazetteerUtils }
import index.PlaceIndex
import models._
import models.content.GeoDocumentTexts
import models.stats.PlaceStats
import play.api.db.slick._

/** Utility object to serialize Annotation data to CSV.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class CSVSerializer extends BaseSerializer {
  
  private val SEPARATOR = ";"
  
  private val UTF8 = "UTF-8"
  
  private def esc(field: String) = 
    field.replace(SEPARATOR, "\\" + SEPARATOR).replace(System.lineSeparator(), "\\n")
    
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
  def serializeAnnotationsConsolidated(gdoc: GeoDocument, annotations: Seq[Annotation], includeCoordinates: Boolean = true, includeFulltext: Boolean = false)(implicit s: Session): String = {
    val meta = {
        Seq("title" -> gdoc.title) ++
        Seq("author" -> gdoc.author,
            "language" -> gdoc.language,
            "date (numeric)" -> gdoc.date, 
            "date" -> gdoc.dateComment,
            "description" -> gdoc.description,
            "external ID" -> gdoc.externalWorkID,
            "collections" -> gdoc.id.map(id => CollectionMemberships.findForGeoDocument(id).mkString(", ")))
        .filter(_._2.isDefined).map(tuple => (tuple._1, tuple._2.get))
      }.map(tuple => "# " + tuple._1 + ": " + tuple._2).mkString("\n")
      
    val fulltexts: Map[Option[Int], String] = 
      if (includeFulltext) {
        val gdocTexts = GeoDocumentTexts.findByGeoDocument(gdoc.id.get)
        if (gdocTexts.isEmpty)
          Map.empty[Option[Int], String]
        else
          gdocTexts.map(t => (t.gdocPartId, new String(t.text, UTF8))).toMap
      } else {
        Map.empty[Option[Int], String]
      }
      
    val header = 
      {
        Seq("toponym", "gazetteer_uri", "gazetteer_label", "lat", "lng", "place_category", "document_part", "status", "tags", "source", "img_x", "img_y") ++ {
         if (includeFulltext) {
           Seq("fulltext_prefix", "fulltext_suffix")
         } else {
           Seq.empty[String]
         }
        }
      }.mkString(SEPARATOR) + SEPARATOR + "\n"
            
    // To obtain the proper fulltext context around an annotation, we need
    // to know the offset of the previous and next annotation as well, so we 
    // (i) add an 'empty' slot at the end of our annotation list and
    // (ii) run a sliding window (currentAnnotation, nextAnnotation) across the list 
    // (iii) fold that to process each 'currentAnnotation' (and pass the the current
    // annotation to the next fold step as the previous annotation)
    val annotationsPadded = annotations.map(Some(_)) :+ None // Here we add the empty at the end
    
    // The (current, next) pairs obtained through sliding
    val currentNextPairs = annotationsPadded.iterator.sliding(2)
    
    // Fold it!
    currentNextPairs.foldLeft((meta + "\n" + header, Option.empty[Annotation])){ case ((csv, previousAnnotation), nextTwoAnnotations) => {
      val currentAnnotation = nextTwoAnnotations.head.get // Must be defined
      val nextAnnotation = nextTwoAnnotations.last
      
      val previousOffset = 
        if (currentAnnotation.gdocPartId == previousAnnotation.flatMap(_.gdocPartId))
          previousAnnotation.map(a => 
            a.correctedOffset.getOrElse(a.offset.getOrElse(0)) + 
            a.correctedToponym.getOrElse(a.toponym.getOrElse("")).size)
        else
          None
          
      val nextOffset =
        if (currentAnnotation.gdocPartId == nextAnnotation.flatMap(_.gdocPartId))
          nextAnnotation.map(a => a.correctedOffset.getOrElse(a.offset.getOrElse(0)))
        else
          None        
    
      
      val row = 
        if (includeFulltext)
          oneAnnotationToCSV(currentAnnotation, includeCoordinates, includeFulltext, previousOffset, nextOffset, fulltexts)
        else
          oneAnnotationToCSV(currentAnnotation, includeCoordinates, includeFulltext)

      (csv + row, Some(currentAnnotation))
    }}._1 // We only need the string as end result
  }
  
  /** Helper function that generates a consolidated CSV row for one annotation with appropriate fulltext snippet, if required **/
  private def oneAnnotationToCSV(annotation: Annotation, includeCoordinates: Boolean, includeFulltext: Boolean,
      fromOffset: Option[Int] = None, toOffset: Option[Int] = None,
      fulltexts:  Map[Option[Int], String] = Map.empty[Option[Int], String])(implicit s: Session): String = {
    
    val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
    
    val uri = 
      if (annotation.status == AnnotationStatus.VERIFIED) {
        if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty)
          annotation.correctedGazetteerURI
        else
          annotation.gazetteerURI
      } else {
        None // We remove any existing URI in case the status is not VERIFIED
    }
    
    val (label, category, coord) = {
      if (includeCoordinates) {
        val queryResult = uri.flatMap(CrossGazetteerUtils.getConflatedPlace(_))
        val label = queryResult.map(_._1.label)
        val category = queryResult.flatMap(_._1.category)
        val coord = queryResult.flatMap(_._2).map(_.getCentroid.getCoordinate)    
        (label, category, coord)
      } else {
        (None, None, None)
      }
    }
      
    val imgCoord = {
      val anchorJson = if (annotation.correctedAnchor.isDefined) annotation.correctedAnchor else annotation.anchor
      if (anchorJson.isDefined) {
        val anchor = new ImageAnchor(anchorJson.get)
        Some((anchor.x, anchor.y))
      } else {
        None
      }
    }
      
    // Get the fulltext for this annotation...
    val fulltext = fulltexts.get(annotation.gdocPartId)      
    
    // ...and clip the corresponding part
    val (fulltextPrefix, fulltextSuffix): (Option[String], Option[String]) = 
      if (includeFulltext) {
        val annotationOffset = annotation.correctedOffset.getOrElse(annotation.offset.getOrElse(0))
        val prefix = fulltext.map(_.substring(fromOffset.getOrElse(0), annotationOffset))
        
        val suffixStartOffset = annotationOffset + toponym.map(_.size).getOrElse(0)
        val suffix = 
          if (toOffset.isDefined)
            fulltext.map(_.substring(suffixStartOffset, toOffset.get))
          else
            fulltext.map(_.substring(suffixStartOffset))
                
        (prefix.map(p => esc(p.replace("\n", " ").trim)), suffix.map(s => esc(s.replace("\n", " ").trim)))
      } else {
        (None, None)
      }
      
    esc(toponym.getOrElse("")) + SEPARATOR + 
    uri.map(uri => PlaceIndex.normalizeURI(uri)).getOrElse("") + SEPARATOR + 
    label.getOrElse("") + SEPARATOR +
    coord.map(_.y).getOrElse("") + SEPARATOR +
    coord.map(_.x).getOrElse("") + SEPARATOR +
    category.map(_.toString).getOrElse("") + SEPARATOR +
    annotation.gdocPartId.map(getPart(_).map(_.title)).flatten.getOrElse("") + SEPARATOR +
    annotation.status.toString + SEPARATOR + 
    { if (annotation.tags.size > 0) "\"" + annotation.tags.mkString(",") + "\"" else "" } + SEPARATOR + 
    getSourceForAnnotation(annotation).getOrElse("") + SEPARATOR +
    imgCoord.map(_._1).getOrElse("") + SEPARATOR +
    imgCoord.map(_._2).getOrElse("") + SEPARATOR +
    { if (includeFulltext) fulltextPrefix.getOrElse("") + SEPARATOR else "" } + 
    { if (includeFulltext) fulltextSuffix.getOrElse("") + SEPARATOR else "" } + "\n"        
  }
  
  /** Generates a full backup of annotations, compatible with Recogito's upload mechanism.
    * 
    * This version of the CSV data exposes all original fields from the annotations table
    * in the database. CSV files of this type can be used to restore data in the DB.
    * @param annotations the annotations
    * @return the CSV
    */
  def serializeAnnotationsAsDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("uuid", "gdoc_part", "status", "toponym", "offset", "anchor", "gazetteer_uri", "toponym_corrected", 
                     "offset_corrected", "anchor_corrected", "gazetteer_uri_corrected", "tags", "comment", "source", "see_also")
                     .mkString(SEPARATOR) + SEPARATOR + "\n"
      
    annotations.foldLeft(header)((csv, annotation) => {      
      csv + 
      annotation.uuid + SEPARATOR +
      annotation.gdocPartId.map(getPart(_).map(_.title)).flatten.getOrElse("") + SEPARATOR +
      annotation.status + SEPARATOR +
      esc(annotation.toponym.getOrElse("")) + SEPARATOR +
      annotation.offset.getOrElse("") + SEPARATOR +
      annotation.anchor.getOrElse("") + SEPARATOR +
      annotation.gazetteerURI.map(PlaceIndex.normalizeURI(_)).getOrElse("") + SEPARATOR +
      esc(annotation.correctedToponym.getOrElse("")) + SEPARATOR +
      annotation.correctedOffset.getOrElse("") + SEPARATOR +
      annotation.correctedAnchor.getOrElse("") + SEPARATOR +
      annotation.correctedGazetteerURI.map(PlaceIndex.normalizeURI(_)).getOrElse("") + SEPARATOR +
      esc(annotation.tags.getOrElse("")) + SEPARATOR +
      esc(annotation.comment.getOrElse("")) + SEPARATOR +
      annotation.source.getOrElse("") + SEPARATOR +
      annotation.seeAlso.mkString(",") + SEPARATOR +
      "\n"
    })
  }
  
  /** Serializes the list of users for backup purposes.
    *
    * @param users the users to serialize to CSV  
    */
  def serializeUsers(users: Seq[User]): String = {
    val header = Seq("username","hash","salt","member_since","editable_documents","is_admin").mkString(SEPARATOR) + SEPARATOR + "\n"
    users.foldLeft(header)((csv, user) => {
      csv +
      esc(user.username) + SEPARATOR +
      user.hash + SEPARATOR +
      user.salt + SEPARATOR +
      user.memberSince.getTime + SEPARATOR +
      user.editableDocuments + SEPARATOR +
      user.isAdmin.toString + SEPARATOR + "\n"
    })
  }

  /** Serializes the edit history for backup purposes.
   *
   * @param history the list of edit events to serialize to CSV  
   */
  def serializeEditHistory(history: Seq[EditEvent]): String = {
    val header =
      Seq("annotation_id","username","timestamp","timestamp_formatted","annotation_before","updated_toponym", "updated_status","updated_uri",
            "updated_tags","updated_comment").mkString(SEPARATOR) + SEPARATOR + "\n"
    
    history.foldLeft(header)((csv, event) => {
      csv +
      event.annotationId + SEPARATOR + 
      esc(event.username) + SEPARATOR +
      event.timestamp.getTime + SEPARATOR + 
      event.timestamp.toString + SEPARATOR +
      event.annotationBefore.getOrElse("") + SEPARATOR + 
      esc(event.updatedToponym.getOrElse("")) + SEPARATOR + 
      event.updatedStatus.getOrElse("") + SEPARATOR + 
      event.updatedURI.getOrElse("") + SEPARATOR +
      esc(event.updatedTags.getOrElse("")) + SEPARATOR +
      esc(event.updatedComment.getOrElse("")) + SEPARATOR + "\n"
    })
  }
  
  def serializeEditHistoryWithDocMeta(history: Seq[(EditEvent, Option[Int])])(implicit s: Session): String = {
    val gdocs = GeoDocuments.findByIds(history.flatMap(_._2).distinct).map(gdoc => (gdoc.id.get, gdoc)).toMap
    val collections = CollectionMemberships.findForGeoDocuments(gdocs.map(_._1).toSeq)
    
    val header = 
      Seq("annotation_id","username","timestamp","timestamp_formatted","annotation_before","updated_toponym", "updated_status","updated_uri",
          "updated_tags","updated_comment", "doc_id", "doc_author", "doc_title", "doc_language", "collections").mkString(SEPARATOR) + SEPARATOR + "\n"
    
    history.foldLeft(header){ case (csv, (event, gdocId)) => {
      val gdoc = gdocId.flatMap(id => gdocs.get(id))
      val coll = gdoc.flatMap(g => collections.get(g.id.get))
      
      csv +
      event.annotationId + SEPARATOR + 
      esc(event.username) + SEPARATOR +
      event.timestamp.getTime + SEPARATOR + 
      event.timestamp.toString + SEPARATOR +
      esc(event.annotationBefore.getOrElse("")) + SEPARATOR + 
      esc(event.updatedToponym.getOrElse("")) + SEPARATOR + 
      event.updatedStatus.getOrElse("") + SEPARATOR + 
      event.updatedURI.getOrElse("") + SEPARATOR +
      esc(event.updatedTags.getOrElse("")) + SEPARATOR +
      esc(event.updatedComment.getOrElse("")) + SEPARATOR +
      gdoc.map(_.id.get.toString).getOrElse("") + SEPARATOR +
      esc(gdoc.flatMap(_.author).getOrElse("")) + SEPARATOR +
      esc(gdoc.map(_.title).getOrElse("")) + SEPARATOR +
      gdoc.flatMap(_.language).getOrElse("") + SEPARATOR +
      coll.map(_.mkString(",")).getOrElse("") + SEPARATOR + "\n"
    }}
  }
  
  /** Serializes the daily stats history for backup purposes.
   *
   * @param stats the list of stats records to serialize to CSV  
   */
  def serializeAnnotationProgressStats(stats: Seq[StatsHistoryRecord]): String = {
    val header = Seq("timestamp","timestamp_formatted","verified_toponyms","unidentifiable_toponyms","total_toponyms","total_edits")
      .mkString(SEPARATOR) + SEPARATOR + "\n"
      
    stats.foldLeft(header)((csv, record) => {
      csv + 
      record.timestamp.getTime + SEPARATOR + 
      record.timestamp.toString + SEPARATOR +
      record.verifiedToponyms + SEPARATOR +
      record.unidentifiableToponyms + SEPARATOR +
      record.totalToponyms + SEPARATOR +
      record.totalEdits + SEPARATOR + "\n"
    })
  }
  
  def serializePlaceStats(stats: PlaceStats): String = {
    val header = Seq("uri", "lon", "lat", "name_in_gazetteer", "number_of_annotations", "toponym_variants").mkString(SEPARATOR) + "\n"
    
    stats.uniquePlaces.foldLeft(header){ case (csv, (place, network, count, toponyms)) => {
      val geometry = CrossGazetteerUtils.getPreferredGeometry(place, network)
      val coord = geometry.map(_.getCentroid.getCoordinate)
      
      csv +
      place.uri + SEPARATOR +
      coord.map(_.x.toString).getOrElse("") + SEPARATOR +
      coord.map(_.y.toString).getOrElse("") + SEPARATOR +
      place.label + SEPARATOR +
      count.toString + SEPARATOR + 
      toponyms.map(_._1).mkString(",") + "\n"
    }}
  }

}
