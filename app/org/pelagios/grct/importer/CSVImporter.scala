package org.pelagios.grct.importer

import models._
import play.api.db.slick._
import scala.io.Source

object CSVImporter {
  
  private val IDX_URI = 1  
  private val IDX_TOPONYM = 2
  private val IDX_OFFSET = 3
  private val IDX_TAGS = 4 

  def importAnnotations(file: String, gdocId: Int, gdocPartId: Int): Seq[Annotation] = {
    val data = Source.fromFile(file).getLines.drop(1).filter(!_.isEmpty)
    data.map(line => {
      val fields = line.split(",", -1)
      val placeURI = if (fields(IDX_URI).isEmpty()) None else Some(fields(IDX_URI)) 
      
      Annotation(None, gdocId, Some(gdocPartId), AnnotationStatus.NOT_VERIFIED,
          Some(fields(IDX_TOPONYM)), Some(fields(IDX_OFFSET).toInt), placeURI, tags = Some(fields(IDX_TAGS)))
    }).toSeq
  }
  
  def importCSV(file: String, gdocId: Int)(implicit s: Session): Seq[Annotation] = {
    val data = Source.fromFile(file).getLines    
    val header = data.take(1).toSeq.head.split(",", -1).toSeq 

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
    
    data.map(_.split(",", -1)).map(fields => {
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