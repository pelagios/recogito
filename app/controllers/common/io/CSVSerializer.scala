package controllers.common.io

import global.{ Global, CrossGazetteerUtils }
import models._
import org.pelagios.gazetteer.GazetteerUtils
import play.api.db.slick._
import models.StatsHistoryRecord

/** Utility object to serialize Annotation data to CSV.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class CSVSerializer extends BaseSerializer {
  
  private val SEPARATOR = ";"
    
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
  def serializeAnnotationsConsolidated(gdoc: GeoDocument, annotations: Seq[Annotation])(implicit s: Session): String = {
    val meta = {
        Seq("title" -> gdoc.title) ++
        Seq("author" -> gdoc.author,
            "language" -> gdoc.language,
            "date (numeric)" -> gdoc.date, 
            "date" -> gdoc.dateComment,
            "description" -> gdoc.description,
            "external ID" -> gdoc.externalWorkID).filter(_._2.isDefined).map(tuple => (tuple._1, tuple._2.get))
      }.map(tuple => "# " + tuple._1 + ": " + tuple._2).mkString("\n")
    
    val header = Seq("toponym","gazetteer_uri","lat","lng", "place_category", "document_part", "status", "tags", "source").mkString(SEPARATOR) + SEPARATOR + "\n"
    
    annotations.foldLeft(meta + "\n" + header)((csv, annotation) => {
      val uri = 
        if (annotation.status == AnnotationStatus.VERIFIED) {
		  if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty)
		    annotation.correctedGazetteerURI
		  else
		    annotation.gazetteerURI
	    } else {
		  None // We remove any existing URI in case the status is not VERIFIED
		}
      val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
      
      val queryResult = uri.flatMap(CrossGazetteerUtils.getPlace(_))
      val category = queryResult.flatMap(_._1.category)
      val coord = queryResult.flatMap(_._2)
        
      csv + 
      esc(toponym.getOrElse("")) + SEPARATOR + 
      uri.map(uri => GazetteerUtils.normalizeURI(uri)).getOrElse("") + SEPARATOR + 
      coord.map(_.y).getOrElse("") + SEPARATOR +
      coord.map(_.x).getOrElse("") + SEPARATOR +
      category.map(_.toString).getOrElse("") + SEPARATOR +
      annotation.gdocPartId.map(getPart(_).map(_.title)).flatten.getOrElse("") + SEPARATOR +
      annotation.status.toString + SEPARATOR + 
      { if (annotation.tags.size > 0) "\"" + annotation.tags.mkString(",") + "\"" else "" } + SEPARATOR + 
      getSourceForAnnotation(annotation).getOrElse("") + SEPARATOR + "\n"
    })
  }
  
  /** Generates a full backup of annotations, compatible with Recogito's upload mechanism.
    * 
    * This version of the CSV data exposes all original fields from the annotations table
    * in the database. CSV files of this type can be used to restore data in the DB.
    * @param annotations the annotations
    * @return the CSV
    */
  def serializeAnnotationsAsDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("uuid", "gdoc_part", "status", "toponym", "offset", "anchor", "gazetteer_uri", "latlon", "place_category", "toponym_corrected", 
                     "offset_corrected", "anchor_corrected", "gazetteer_uri_corrected", "latlon_corrected", "place_category_corrected", "tags", "comment", "source", "see_also")
                     .mkString(SEPARATOR) + SEPARATOR + "\n"
      
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
      esc(annotation.toponym.getOrElse("")) + SEPARATOR +
      annotation.offset.getOrElse("") + SEPARATOR +
      annotation.anchor.getOrElse("") + SEPARATOR +
      annotation.gazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      coordinate.map(c => c.x + "," + c.y).getOrElse("") + SEPARATOR +
      placeCategory.map(_.toString).getOrElse("") + SEPARATOR +
      esc(annotation.correctedToponym.getOrElse("")) + SEPARATOR +
      annotation.correctedOffset.getOrElse("") + SEPARATOR +
      annotation.correctedAnchor.getOrElse("") + SEPARATOR +
      annotation.correctedGazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      correctedCoordinate.map(c => c.x + "," + c.y).getOrElse("") + SEPARATOR +
      correctedPlaceCategory.map(_.toString).getOrElse("") + SEPARATOR +
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
      Seq("annotation_id","username","timestamp","timestamp_formatted","annotation_before","updated_toponym", "updated_status","updated_uri","updated_tags","updated_comment")
      .mkString(SEPARATOR) + SEPARATOR + "\n"
   
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
  
  /** Serializes the daily stats history for backup purposes.
   *
   * @param stats the list of stats records to serialize to CSV  
   */
  def serializeStats(stats: Seq[StatsHistoryRecord]): String = {
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
  
  private def esc(field: String) = 
    field.replace(SEPARATOR, "\\" + SEPARATOR).replace(System.lineSeparator(), "\\n")

}
