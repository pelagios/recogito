package org.pelagios.grct.exporter

import models._
import java.io.InputStream
import org.pelagios.gazetteer.GazetteerUtils
import org.pelagios.grct.Global
import play.api.db.slick._

object CSVExporter {
  
  /** Generates CSV for 'public consumption' **/
  def toCSV(annotations: Seq[Annotation]): String = {
    val header = "toponym,uri,lat,lng\n"
    annotations.foldLeft(header)((csv, annotation) => {
      val uri = if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.isEmpty) annotation.correctedGazetteerURI else annotation.gazetteerURI
      val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
      if (uri.isDefined && !uri.get.isEmpty) {
        val coord = Global.index.getPlace(uri.get).map(_.getCentroid).flatten
        csv + 
        toponym.getOrElse("") + "," + 
        GazetteerUtils.normalizeURI(uri.get) + "," + 
        coord.map(_.y).getOrElse("") + "," +
        coord.map(_.x).getOrElse("") + "\n"
      } else {
        csv
      }
    })
  }
  
  /** Creates a full CSV dump, reflecting the DB table structure **/
  def toDump(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = "gdoc_part,status,toponym,offset,gazetteer_uri,toponym_corrected,offset_corrected,gazetteer_uri_corrected,tags,comment\n"
      
    annotations.foldLeft(header)((csv, annotation) => {
      csv + 
      annotation.gdocPartId.map(GeoDocumentParts.getTitle(_)).flatten.getOrElse("") + "," +
      annotation.status + "," +
      annotation.toponym.getOrElse("") + "," +
      annotation.offset.getOrElse("") + "," +
      annotation.gazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + "," +
      annotation.correctedToponym.getOrElse("") + "," +
      annotation.correctedOffset.getOrElse("") + "," +
      annotation.correctedGazetteerURI.map(GazetteerUtils.normalizeURI(_)).getOrElse("") + "," +
      annotation.tags.getOrElse("") + "," +
      annotation.comment.getOrElse("") + "," +
      "\n"
    })
  }

}