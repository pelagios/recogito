package controllers.common.io

import models.{ Annotation, GeoDocument }
import models.content.GeoDocumentText

class TextSerializer {
  
  private val UTF8 = "UTF-8"
  
  private val SEPARATOR = ";"
  
  private val NEWLINE = "\n"
  
  def serialize(gdoc: GeoDocument, gdocText: GeoDocumentText, annotations: Seq[Annotation]): String = {
    if (!gdocText.renderAsTable)
      throw new UnsupportedOperationException("Recogito currently only supports CSV document export")
    
    val lines = new String(gdocText.text, UTF8).split(NEWLINE).map(_.trim).filter(!_.startsWith("#"))
    
    // Prepend gazetteer match to each line
    val header = "close_match" + SEPARATOR + lines.head + NEWLINE
    lines.tail.zip(annotations).foldLeft(header){ case (csv, (nextLine, annotation)) => {
      val gazetteerURI = 
        if (annotation.correctedGazetteerURI.isDefined)
          annotation.correctedGazetteerURI
        else
          annotation.gazetteerURI
          
      csv + 
      gazetteerURI.getOrElse("") + SEPARATOR +
      nextLine + NEWLINE
    }}
  }

}