package controllers.io

import models._
import play.api.db.slick._
import scala.io.Source
import play.api.Logger

/** Utility object to convert CSV input data to Annotation objects.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object CSVParser {
  
  private val SEPARATOR = ";"
  
  /** Parses an input CSV file and produces annotations.
    * 
    * Since in the data model, annotations cannot exist without a valid parent
    * document, it is required to specify a GeoDocument ID on import.
    * @param file the CSV file path
    * @param gdocId the database ID of the GeoDocument to import to
    */
  def parse(file: String, gdocId: Int)(implicit s: Session): Seq[Annotation] = {
    val data = Source.fromFile(file).getLines    
    val header = data.take(1).toSeq.head.split(SEPARATOR, -1).toSeq 
    
    // Helper method to find the row index for a specific label
    def idx(label: String): Option[Int] = header.indexWhere(_.equalsIgnoreCase(label)) match {
      case -1 => None
      case idx => Some(idx)
    }
    
    val idxGdocPart = idx("gdoc_part")
    val idxStatus = idx("status")
    val idxToponym = idx("toponym")
    val idxOffset = idx("offset")
    val idxGazetteerURI = idx("gazetteer_uri")
    val idxCorrectedToponym = idx("toponym_corrected")
    val idxCorrectedOffset = idx("offset_corrected")
    val idxCorrectedGazetteerURI = idx("gazetteer_uri_corrected")
    val idxTags = idx("tags")
    val idxComment = idx("comment")
        
    // Helper function to turn optional fields to Option[String]
    def parseOptCol(idx: Option[Int])(implicit fields: Array[String]): Option[String] = {
      if (idx.isDefined) {
        val string = fields(idx.get)
        if (string.trim.isEmpty) 
          None // The field is in the CSV, but the string is empty -> None 
        else
          Some(string) // Field is there & contains a string
      } else {
        // If the field is not in the CSV at all -> None
        None
      }
    }
    
    data.map(_.split(SEPARATOR, -1)).map(implicit fields => {
      Annotation(
          None,
          gdocId,
          GeoDocumentParts.getId(fields(idxGdocPart.get)), // Note: we strictly require gdoc_part to be set by convention!
          parseOptCol(idxStatus).map(AnnotationStatus.withName(_)).getOrElse(AnnotationStatus.NOT_VERIFIED),
          parseOptCol(idxToponym),
          parseOptCol(idxOffset).map(_.toInt),
          parseOptCol(idxGazetteerURI),
          parseOptCol(idxCorrectedToponym),
          parseOptCol(idxCorrectedOffset).map(_.toInt),
          parseOptCol(idxCorrectedGazetteerURI),
          parseOptCol(idxTags),
          parseOptCol(idxComment))
    }).toSeq
  }
  
}