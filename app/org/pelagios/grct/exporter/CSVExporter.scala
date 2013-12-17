package org.pelagios.grct.exporter

import models.Annotation
import java.io.InputStream
import org.pelagios.gazetteer.GazetteerUtils
import org.pelagios.grct.Global
import play.api.Logger

object CSVExporter {
  
  def toCSV(annotations: Seq[Annotation]): String = {
    val header = "toponym,uri,lat,lng"
    annotations.foldLeft(header + "\n")((csv, annotation) => {
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

}