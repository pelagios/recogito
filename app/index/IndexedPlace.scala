package index

import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.io.StringWriter
import org.geotools.geojson.geom.GeometryJSON
import org.pelagios.api.PlainLiteral
import org.pelagios.api.gazetteer.{ Place, PlaceCategory }
import org.pelagios.api.gazetteer.patch._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class IndexedPlace(json: String) {
  
  override val toString = json
  
  lazy val asJson = Json.parse(json).as[JsObject]
    
  lazy val uri: String = (asJson \ "uri").as[String] 
  
  lazy val label: String = (asJson \ "label").as[String]
  
  lazy val description: Option[String] = (asJson \ "description").asOpt[String] 
  
  lazy val category: Option[PlaceCategory.Category] = ((asJson \ "category").asOpt[String]).map(PlaceCategory.withName(_))
  
  lazy val names: List[PlainLiteral] = (asJson \ "names").as[List[JsObject]]
    .map(literal => {
      val chars = (literal \ "chars").as[String]
      val lang = (literal \ "lang").asOpt[String]
      PlainLiteral(chars, lang)
    })
  
  lazy val geometryJson: Option[JsValue] = (asJson \ "geometry").asOpt[JsValue]
  
  lazy val geometry: Option[Geometry] = geometryJson
    .map(geoJson => new GeometryJSON().read(Json.stringify(geoJson).trim))
    
  lazy val geometryWKT: Option[String] =  geometry.map(geom => new WKTWriter().write(geom))
  
  lazy val centroid: Option[Coordinate] = geometry.map(_.getCentroid.getCoordinate)
  
  lazy val closeMatches: List[String] = (asJson \ "close_matches").as[List[String]]
  
  lazy val exactMatches: List[String] = (asJson \ "exact_matches").as[List[String]]
  
  lazy val matches: List[String] = closeMatches ++ exactMatches
  
  def patch(patch: PlacePatch, config: PatchConfig): IndexedPlace = 
    patchGeometry(patch, config.geometryStrategy).patchNames(patch, config.namesStrategy)
    
  private def patchGeometry(patch: PlacePatch, strategy: PatchStrategy.Value): IndexedPlace = {
    val geometry = patch.locations.headOption.map(location => Json.parse(location.geoJSON))
    if (geometry.isDefined)
      strategy match {
        case PatchStrategy.REPLACE => {
          val updatedJson = (asJson - "geometry") + ("geometry" -> geometry.get) 
          new IndexedPlace(Json.stringify(updatedJson))
        }
        case PatchStrategy.APPEND => throw new UnsupportedOperationException // We don't support append at this time
      }
    else
      this
  }
  
  private def patchNames(patch: PlacePatch, strategy: PatchStrategy.Value): IndexedPlace = {    
    if (patch.names.size > 0) {
      import IndexedPlace.plainLiteralWrites
      val names = Json.toJson(patch.names).as[JsArray] 
      
      strategy match {
        case PatchStrategy.REPLACE => {
          val updatedJson = (asJson - "names") + ("names" -> names)
          new IndexedPlace(Json.stringify(updatedJson))  
        }
        
        case PatchStrategy.APPEND => {
          val updatedNames = (asJson \ "names").as[JsArray] ++ names
          val updatedJson = (asJson - "names") + ("names" -> updatedNames)
          new IndexedPlace(Json.stringify(updatedJson))            
        }
      }
    } else {
      this
    }
  }
  
}

object IndexedPlace { 
   
  /** JSON Writes **/
  
  private implicit val plainLiteralWrites: Writes[PlainLiteral] = (
    (JsPath \ "chars").write[String] ~
    (JsPath \ "lang").writeNullable[String]
  )(l => (l.chars, l.lang))
  
  private implicit val placeWrites: Writes[Place] = (
    (JsPath \ "uri").write[String] ~
    (JsPath \ "label").write[String] ~
    (JsPath \ "description").writeNullable[String] ~
    (JsPath \ "category").writeNullable[String] ~
    (JsPath \ "names").write[Seq[PlainLiteral]] ~
    (JsPath \ "geometry").writeNullable[JsValue] ~
    (JsPath \ "close_matches").write[Seq[String]] ~
    (JsPath \ "exact_matches").write[Seq[String]]
  )(p  => (
      PlaceIndex.normalizeURI(p.uri),
      p.label,
      p.descriptions.headOption.map(_.chars),
      p.category.map(_.toString),
      p.names,
      p.locations.headOption.map(location => Json.parse(location.geoJSON)),
      p.closeMatches.map(PlaceIndex.normalizeURI(_)),
      p.exactMatches.map(PlaceIndex.normalizeURI(_))))
  
  implicit def geometry2geoJSON(geometry: Geometry): JsValue = {
    val writer = new StringWriter()
    new GeometryJSON().write(geometry, writer)
    Json.parse(writer.toString)
  }

  def toIndexedPlace(place: Place): IndexedPlace = {
    val json = Json.toJson(place)
    new IndexedPlace(Json.stringify(json))
  }
    
}