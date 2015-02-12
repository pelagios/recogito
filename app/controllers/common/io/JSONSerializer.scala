package controllers.common.io

import global.{ CrossGazetteerUtils, Global }
import models._
import models.content._
import play.api.db.slick._
import play.api.libs.json.{ Json, JsObject }
import org.pelagios.api.gazetteer.Place
import com.vividsolutions.jts.geom.Coordinate
import controllers.common.ImageAnnotationSorter
import play.api.Logger
import com.vividsolutions.jts.geom.Point

/** Utility object to serialize Annotation data to JSON.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class JSONSerializer extends BaseSerializer {
   
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
  def toJson(a: Annotation, includePlaces: Boolean, includeContext: Boolean, includeLastEdit: Boolean)(implicit session: Session): JsObject = {    
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
    
    val lastEdit = if (includeLastEdit)
        EditHistory.findByAnnotation(a.uuid, 1).headOption
      else
        None

    Json.obj(
      "id" -> a.uuid.toString,
      "toponym" -> toponym,
      "status" -> a.status.toString,
      "last_edit" -> lastEdit.map(event => Json.obj(
        "username" -> event.username,
        "timestamp" -> event.timestamp.getTime)),
      "place" -> { if (includePlaces) a.gazetteerURI.map(placeUriToJson(_)) else a.gazetteerURI },
      "place_fixed" -> { if (includePlaces) a.correctedGazetteerURI.map(placeUriToJson(_)) else a.correctedGazetteerURI },
      "tags" -> a.tags.map(_.split(",")),
      "context" -> context,
      "comment" -> a.comment,
      "source" -> getSourceForAnnotation(a))
  }
  
  /** Serializes a single GeoDocument, optionally with annotations in-lined.
    *  
    * Note that the addition of in-lined annotations is an expensive operation and
    * will result in large JSON files! 
    * @param doc the GeoDocument
    * @param includeAnnotations whether to include the annotations in the JSON
    */  
  def toJson(doc: GeoDocument, includeAnnotations: Boolean, verifiedOnly: Boolean)(implicit session: Session): JsObject = {
    val startTime = System.currentTimeMillis
    val annotations = if (includeAnnotations) {
      val all = 
        if (verifiedOnly) {
          Annotations.findByGeoDocumentAndStatus(doc.id.get, AnnotationStatus.VERIFIED)
        } else {
          Annotations.findByGeoDocument(doc.id.get)
        }
      
      // Check if these are image annotations - per convention, we assume either ALL or NONE of the annotations are image annotations
      val imageAnnotations = all.filter(a => a.anchor.isDefined || a.correctedAnchor.isDefined)
      
      if (imageAnnotations.isEmpty) {
        Some(all)
      } else {
        Some(ImageAnnotationSorter.sortByNearestNeighbour(all))
      }
    } else {
      None
    }
    val parts = GeoDocumentParts.findByGeoDocument(doc.id.get)
    
    val result = if (parts.size == 0) {
      Json.obj(
        "id" -> doc.id,
        "title" -> doc.title,
        "source" -> doc.source,
        "annotations" -> annotations.map(_.map(toJson(_, true, false, false)))
      )
    } else {
      val annotationsByPart = annotations.map(_.groupBy(_.gdocPartId))
      Json.obj(
        "id" -> doc.id,
        "title" -> doc.title,
        "parts" -> parts.map(part => {          
          Json.obj(
            "title" -> part.title,
            "source" -> part.source,
            "annotations" -> {
              annotationsByPart.flatMap(byPartMap => {
                byPartMap.get(part.id)
              }).getOrElse(Seq.empty[Annotation]).map(toJson(_, true, false, false))
            }
          )
        }
      ))
    }
    
    result
  }
  
  /** Renders a JSON object for the place with the specified gazetteer URI **/
  private def placeUriToJson(uri: String): Option[JsObject] = {
    val p = getPlace(uri)
    
    if (p.isDefined) {
      val (place, location) = p.get
      Some(Json.obj(
        "uri" -> place.uri,
        "title" -> place.label,
        "description" -> place.descriptions.map(_.chars).mkString(", "),
        "names" -> Json.toJson(place.names.map(_.chars)),
        "category" -> place.category.map(_.toString),
        "coordinate" -> location.map(l => { 
          val coord = l.geometry.getCentroid.getCoordinate
          Json.toJson(Seq(coord.y, coord.x)) 
         }),
        "geometry" -> location.flatMap(l => { 
          l.geometry match {
            case g: Point => None
            case _ => Some(Json.parse(l.geoJSON))
          }
        })        
      ))      
    } else {
      None
    }
  }

  
}
