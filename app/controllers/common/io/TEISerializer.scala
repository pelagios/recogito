package controllers.common.io

import models.{ Annotation, Annotations, AnnotationStatus, GeoDocument }
import models.content.GeoDocumentTexts
import play.api.db.slick.Config.driver.simple._
import scala.xml.{ Node, Text }

object TEISerializer {
  
  private val UTF8 = "UTF-8"
  
  private def escapePlaintext(segment: String): String = {
    // Should cover most cases (?) - otherwise switch to Apache Commons StringEscapeUtils
    segment
      .replace("<", "&lt;")
      .replace(">", "&gt;")
  }
  
  def serializePart(plaintext: String, annotations: Seq[Annotation]): Seq[Node] = {
    val ranges = annotations.foldLeft((Seq.empty[Node], 0)) { case ((nodes, beginIndex), annotation) =>
      if (annotation.status == AnnotationStatus.FALSE_DETECTION) {
        (nodes, beginIndex)
      } else {
        val toponym = if (annotation.correctedToponym.isDefined) annotation.correctedToponym else annotation.toponym
        val offset = if (annotation.correctedOffset.isDefined) annotation.correctedOffset else annotation.offset 
        val gazetteerUrl = 
          if (annotation.correctedGazetteerURI.isDefined && !annotation.correctedGazetteerURI.get.trim.isEmpty) 
            annotation.correctedGazetteerURI
          else
            annotation.gazetteerURI
            
        if (toponym.isDefined && offset.isDefined) {
          val placeName = gazetteerUrl match {
            case Some(url) => <placeName ref={ url }>{ escapePlaintext(toponym.get) }</placeName>
            case None => <placeName>{ escapePlaintext(toponym.get) }</placeName>
          }
          
          val nextNodes = 
            Seq(new Text(escapePlaintext(plaintext.substring(beginIndex, offset.get))), placeName)

          (nodes ++ nextNodes, offset.get + toponym.get.size)
        } else { 
          (nodes, beginIndex)
        }
      }
    }
    
    val suffix = escapePlaintext(plaintext.substring(ranges._2))
    ranges._1 :+ new Text(suffix)
  }
  
  def serialize(gdoc: GeoDocument)(implicit s: Session) = {
    val texts = GeoDocumentTexts.findByGeoDocument(gdoc.id.get)
    
    val title = 
      if (gdoc.author.isDefined) 
        gdoc.author.get + ", " + gdoc.title
      else
        gdoc.title
        
    val date = (gdoc.date, gdoc.dateComment) match {
      case (Some(date), Some(dateComment)) => <p><date when={ date.toString}>{ dateComment }</date></p>
      case (Some(date), None) => <p><date when={ date.toString}>{ date.toString }</date></p>
      case (None, Some(dateComment)) => <p>{ dateComment }</p>
      case _ => <p/>
    }
    
    val source = gdoc.source match {
      case Some(s) => <p><link target={s} /></p>
      case None => <p>Unknown Source</p>
    }

    val divs = texts.map(text => {
      val plaintext =  new String(text.text, UTF8)
      val annotations =
        if (text.gdocPartId.isDefined)
          Annotations.findByGeoDocumentPart(text.gdocPartId.get)
        else
          Annotations.findByGeoDocument(gdoc.id.get)
          
      <div><p>{ serializePart(plaintext, annotations) }</p></div>
    })

    <TEI xmlns="http://www.tei-c.org/ns/1.0">
      <teiHeader>
        <fileDesc>
          <titleStmt><title>{ title }</title></titleStmt>
          <publicationStmt>{ date }</publicationStmt>
          <sourceDesc>{ source }</sourceDesc>
        </fileDesc>
      </teiHeader>
      <text>
        <body>{ divs }</body>
      </text>
    </TEI>   
  }
  
}