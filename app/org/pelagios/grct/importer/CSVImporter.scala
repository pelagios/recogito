package org.pelagios.grct.importer

import scala.io.Source
import models.Annotation
import models.AnnotationStatus
import play.api.Logger

object CSVImporter {
  
  private val IDX_URI = 1  
  private val IDX_TOPONYM = 4

  def importAnnotations(file: String, gdocPartId: Int): Seq[Annotation] = {
    val data = Source.fromFile(file).getLines.drop(1).filter(!_.isEmpty)
    data.map(line => {
      Logger.info(line)
      val fields = line.split(",", -1)
      val placeURI = if (fields(IDX_URI).isEmpty()) None else Some(fields(IDX_URI)) 
      Annotation(None, fields(IDX_TOPONYM), AnnotationStatus.NOT_VERIFIED, placeURI, None, None, gdocPartId)
    }).toSeq
  }
  
}