package controllers.io

import global.Global
import models._
import org.pelagios.gazetteer.GazetteerUtils
import play.api.db.slick._

/** Utility object to serialize Annotation data to CSV.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object CSVSerializer {
  
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
  def asConsolidatedVerifiedResult(annotations: Seq[Annotation]): String = {
    val header = "toponym,uri,lat,lng\n"
    annotations.foldLeft(header)((csv, annotation) => {
      val uri = if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty) annotation.correctedGazetteerURI else annotation.gazetteerURI
      val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
      if (uri.isDefined && !uri.get.isEmpty) {
        val coord = Global.index.findByURI(uri.get).map(_.getCentroid).flatten
        csv + 
        toponym.getOrElse("") + SEPARATOR + 
        GazetteerUtils.normalizeURI(uri.get) + SEPARATOR + 
        coord.map(_.y).getOrElse("") + SEPARATOR +
        coord.map(_.x).getOrElse("") + "\n"
      } else {
        csv
      }
    })
  }
  
  /** Generates a full backup annotationsf from the database.
    * 
    * This version of the CSV data exposes all original fields from the annotations table
    * in the database. CSV files of this type can be used to restore data in the DB.
    * @param annotations the annotations
    * @return the CSV
    */
  def asDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("id", "gdoc_part", "status", "toponym", "offset", "gazetteer_uri", "toponym_corrected", 
                     "offset_corrected", "gazetteer_uri_corrected", "tags", "comment").mkString(SEPARATOR) + "\n"
      
    annotations.foldLeft(header)((csv, annotation) => {
      csv + 
      annotation.id.get + SEPARATOR +
      annotation.gdocPartId.map(GeoDocumentParts.getTitle(_)).flatten.getOrElse("") + SEPARATOR +
      annotation.status + SEPARATOR +
      annotation.toponym.getOrElse("") + SEPARATOR +
      annotation.offset.getOrElse("") + SEPARATOR +
      annotation.gazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      annotation.correctedToponym.getOrElse("") + SEPARATOR +
      annotation.correctedOffset.getOrElse("") + SEPARATOR +
      annotation.correctedGazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + SEPARATOR +
      annotation.tags.getOrElse("") + SEPARATOR +
      annotation.comment.getOrElse("") + SEPARATOR +
      "\n"
    })
  }

}