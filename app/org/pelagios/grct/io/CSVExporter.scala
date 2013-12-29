package org.pelagios.grct.io

import models._
import org.pelagios.gazetteer.GazetteerUtils
import org.pelagios.grct.Global
import play.api.db.slick._

object CSVExporter {
  
  private val SEPARATOR = ";"
  
  /** Generates CSV for 'public consumption' **/
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
  
  /** Creates a full CSV dump, reflecting the DB table structure **/
  def asDBBackup(annotations: Seq[Annotation])(implicit s: Session): String = {
    val header = Seq("gdoc_part", "status", "toponym", "offset", "gazetteer_uri", "toponym_corrected", 
                     "offset_corrected", "gazetteer_uri_corrected", "tags", "comment").mkString(SEPARATOR) + "\n"
      
    annotations.foldLeft(header)((csv, annotation) => {
      csv + 
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