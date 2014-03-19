package controllers.io

import global.{ Global, CrossGazetteerUtils }
import models._
import org.pelagios.gazetteer.GazetteerUtils
import play.api.db.slick._

/** Utility object to serialize Annotation data to CSV.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class CSVSerializer extends BaseSerializer {
  
  private val SEPARATOR = ";"
    
  /** Serializes the list of users for backup purposes.
    *
    * @param users the users to serialize to CSV  
    */
  def serializeUsers(users: Seq[User]): String = {
    val header = Seq("username","hash","salt","editable_documents","is_admin").mkString(SEPARATOR) + SEPARATOR + "\n"
    users.foldLeft(header)((csv, user) => {
      csv +
      esc(user.username) + SEPARATOR +
      user.hash + SEPARATOR +
      user.salt + SEPARATOR +
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
      Seq("annotation_id","username","timestamp","annotation_before","updated_toponym", "updated_status","updated_uri","updated_tags","updated_comment")
      .mkString(SEPARATOR) + ";\n"
   
    history.foldLeft(header)((csv, event) => {
      csv +
      event.annotationId + SEPARATOR + 
      esc(event.username) + SEPARATOR +
      event.timestamp.getTime() + SEPARATOR + 
      event.annotationBefore.getOrElse("") + SEPARATOR + 
      esc(event.updatedToponym.getOrElse("")) + SEPARATOR + 
      event.updatedStatus.getOrElse("") + SEPARATOR + 
      event.updatedURI.getOrElse("") + SEPARATOR +
      esc(event.updatedTags.getOrElse("")) + SEPARATOR +
      esc(event.updatedComment.getOrElse("")) + SEPARATOR + "\n"
    })
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
  def serializeAnnotationsConsolidated(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("toponym","uri","lat","lng", "place_category", "tags", "source").mkString(SEPARATOR) + SEPARATOR + "\n"
    annotations.foldLeft(header)((csv, annotation) => {
      val uri = if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty) annotation.correctedGazetteerURI else annotation.gazetteerURI
      val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
      
      if (uri.isDefined && !uri.get.isEmpty) {
        val queryResult = CrossGazetteerUtils.getPlace(uri.get)
        val category = queryResult.map(_._1.category).flatten
        val coord = queryResult.map(_._2).flatten
        
        csv + 
        esc(toponym.getOrElse("")) + SEPARATOR + 
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
  
  /** Generates a full backup of annotations, compatible with Recogito's upload mechanism.
    * 
    * This version of the CSV data exposes all original fields from the annotations table
    * in the database. CSV files of this type can be used to restore data in the DB.
    * @param annotations the annotations
    * @return the CSV
    */
  def serializeAnnotationsAsDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("uuid", "gdoc_part", "status", "toponym", "offset", "gazetteer_uri", "latlon", "place_category", "toponym_corrected", 
                     "offset_corrected", "gazetteer_uri_corrected", "latlon_corrected", "place_category_corrected", "tags", "comment", "source", "see_also")
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
      annotation.gazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      coordinate.map(c => c.x + "," + c.y).getOrElse("") + SEPARATOR +
      placeCategory.map(_.toString).getOrElse("") + SEPARATOR +
      esc(annotation.correctedToponym.getOrElse("")) + SEPARATOR +
      annotation.correctedOffset.getOrElse("") + SEPARATOR +
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
  
  private def esc(field: String) = 
    field.replace(SEPARATOR, "\\" + SEPARATOR).replace(System.lineSeparator(), "\\n")

}