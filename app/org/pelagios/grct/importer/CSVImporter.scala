package org.pelagios.grct.importer

import scala.io.Source
import models.Annotation
import models.AnnotationStatus
import play.api.Logger

object CSVImporter {
  
  private val IDX_URI = 1  
  private val IDX_TOPONYM = 2
  private val IDX_OFFSET = 3

  def importAnnotations(file: String, gdocId: Int, gdocPartId: Int): Seq[Annotation] = {
    val data = Source.fromFile(file).getLines.drop(1).filter(!_.isEmpty)
    data.map(line => {
      Logger.info(line)
      val fields = line.split(",", -1)
      val placeURI = if (fields(IDX_URI).isEmpty()) None else Some(fields(IDX_URI)) 
      
      Annotation(None, gdocId, Some(gdocPartId), AnnotationStatus.NOT_VERIFIED,
          Some(fields(IDX_TOPONYM)), Some(fields(IDX_OFFSET).toInt), placeURI)
    }).toSeq
  }
  
}