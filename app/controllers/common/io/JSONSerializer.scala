package controllers.common.io

import global.{ CrossGazetteerUtils, Global }
import models._
import models.content._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsObject }
import play.api.Logger

/** Utility object to serialize Annotation data to JSON.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object JSONSerializer {
   
  private val UTF8 = "UTF-8"  
  private val CONTEXT_SIZE = 80
  
  def toJson(annotations: Seq[Annotation]): Seq[JsObject] = {
    annotations.map(a => {
      val anchor = if (a.correctedAnchor.isDefined) a.correctedAnchor else a.anchor      
      Json.obj(
        "id" -> a.uuid.toString,
        "status" -> a.status.toString,
        "toponym" -> { if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym },
        "comment" -> a.comment,
        "shapes" -> anchor.map(anchor => Json.toJson(Seq(Json.parse(anchor)))))
    })
  }

  /** Serializes a single annotation, with optional fulltext context.
    *  
    * Optionally, fulltext context is pulled from the database, if available. Note 
    * that the addition of fulltext context is an expensive operation! 
    * @param a the annotation
    * @param includeContext whether to include fulltext context or not
    */
  def toJson(a: Annotation, includePlaces: Boolean, includeContext: Boolean)(implicit session: Session): JsObject = {    
    val toponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym
    val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
    val context = if (includeContext) { 
      if (toponym.isDefined && offset.isDefined) {
        val gdocText = GeoDocumentTexts.getTextForAnnotation(a)
        if (gdocText.isDefined) {
          val text = new String(gdocText.get.text, UTF8)
        
          val ctxStart = if (offset.get - CONTEXT_SIZE > -1) offset.get - CONTEXT_SIZE else 0
          val ctxEnd = 
            if (offset.get + toponym.get.size + CONTEXT_SIZE <= text.size) 
              offset.get + toponym.get.size + CONTEXT_SIZE
            else
              text.size
       
          Some(text.substring(ctxStart, ctxEnd).replaceAll("\n+", " ").trim)
        } else {
          None
        }
      } else {
        None
      }
    } else {
      None
    }
    
    val source = if (a.source.isDefined)
                   a.source
                 else if (a.gdocPartId.isDefined)
                   GeoDocumentParts.findById(a.gdocPartId.get).flatMap(_.source)
                 else if (a.gdocId.isDefined)
                   GeoDocuments.findById(a.gdocId.get).flatMap(_.source)
                 else
                   None
                   
    val lastEdit = EditHistory.findByAnnotation(a.uuid, 1).headOption

    Json.obj(
      "id" -> a.uuid.toString,
      "toponym" -> { if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym },
      "status" -> a.status.toString,
      "last_edit" -> lastEdit.map(event => Json.obj(
        "username" -> event.username,
        "timestamp" -> event.timestamp.getTime)),
      "place" -> { if (includePlaces) a.gazetteerURI.map(placeUriToJson(_)) else a.gazetteerURI },
      "place_fixed" -> { if (includePlaces) a.correctedGazetteerURI.map(placeUriToJson(_)) else a.correctedGazetteerURI },
      "tags" -> a.tags.map(_.split(",")),
      "context" -> context,
      "comment" -> a.comment,
      "source" -> source)
  }
  
  /** Serializes a single GeoDocument, optionally with annotations in-lined.
    *  
    * Note that the addition of in-lined annotations is an expensive operation and
    * will result in large JSON files! 
    * @param doc the GeoDocument
    * @param includeAnnotations whether to include the annotations in the JSON
    */  
  def toJson(doc: GeoDocument, includeAnnotations: Boolean)(implicit session: Session): JsObject = {
    val parts = GeoDocumentParts.findByGeoDocument(doc.id.get)
    if (parts.size == 0) {
      Json.obj(
        "id" -> doc.id,
        "title" -> doc.title,
        "source" -> doc.source,
        "annotations" -> { 
          val annotations = 
            if (includeAnnotations)
              Some(Annotations.findByGeoDocument(doc.id.get).map(toJson(_, true, false)))
            else
              None
           
          annotations
        }
      )
    } else {
      Json.obj(
        "id" -> doc.id,
        "title" -> doc.title,
        "parts" -> GeoDocumentParts.findByGeoDocument(doc.id.get).map(part => Json.obj(
          "title" -> part.title,
          "source" -> part.source,
          "annotations" -> { 
            val annotations = 
              if (includeAnnotations)
                Some(Annotations.findByGeoDocumentPart(part.id.get).map(toJson(_, true, false)))
              else
                None
           
            annotations
          }
        )
      ))
    }
  }
    
  /** Renders a JSON object for the place with the specified gazetteer URI **/
  private def placeUriToJson(uri: String): Option[JsObject] = {
    val place = CrossGazetteerUtils.getPlace(uri)
    
    if (place.isDefined) {
      val p = place.get._1
      Some(Json.obj(
        "uri" -> p.uri,
        "title" -> p.label,
        "description" -> p.descriptions.map(_.chars).mkString(", "),
        "names" -> p.names.map(_.chars).mkString(", "),
        "category" -> p.category.map(_.toString),
        "coordinate" -> place.get._2.map(coords => Json.toJson(Seq(coords.y, coords.x)))
      ))      
    } else {
      None
    }
  }
  
}