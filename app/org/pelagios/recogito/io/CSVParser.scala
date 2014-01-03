package org.pelagios.recogito.io

import models._
import play.api.db.slick._
import scala.io.Source

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
    def idx(label: String): Int = header.indexWhere(_.equalsIgnoreCase(label))
    
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
    
    implicit def toOption(string: String) = if (string.trim.isEmpty) None else Some(string)
    
    data.map(_.split(SEPARATOR, -1)).map(fields => {
      Annotation(
          None,
          gdocId,
          GeoDocumentParts.getId(fields(idxGdocPart)),
          AnnotationStatus.withName(fields(idxStatus)),
          fields(idxToponym),
          toOption(fields(idxOffset)).map(_.toInt),
          fields(idxGazetteerURI),
          fields(idxCorrectedToponym),
          toOption(fields(idxCorrectedOffset)).map(_.toInt),
          fields(idxCorrectedGazetteerURI),
          fields(idxTags),
          fields(idxComment))
    }).toSeq
  }
  
}