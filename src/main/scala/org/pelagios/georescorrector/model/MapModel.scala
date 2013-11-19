package org.pelagios.georescorrector.model

import com.vividsolutions.jts.geom.Coordinate
import org.pelagios.georescorrector.index.PlaceIndex
import org.pelagios.georescorrector.Global
import play.api.libs.json._

class MapModel(csv: Seq[CSVModel]) {
  
  val index = Global.index
  
  private val IDX_TOPONYM = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("toponym"))
  private val IDX_GAZETTEER_URI = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("gazetteer uri"))
  private val IDX_GAZETTEER_URI_CORRECTED = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("gazetteer uri (verified/corrected)"))
  
  def toJSON: String = {  
    val parts = csv.map(csv => {
      val places = csv.rows.map(row => {
        // Grab data from the CSV row
        val toponym           = if (IDX_TOPONYM > 0 && row(IDX_TOPONYM) != null) 
                                  Some(row(IDX_TOPONYM)) 
                                else 
                                  None
                        
        val gazetteerURI      = if (IDX_GAZETTEER_URI > 0 && row(IDX_GAZETTEER_URI) != null)
                                  Some(row(IDX_GAZETTEER_URI)) 
                                else 
                                  None
                                  
        val gazetteerURIFixed = if (IDX_GAZETTEER_URI_CORRECTED > 0 && row(IDX_GAZETTEER_URI_CORRECTED) != null) 
                                  Some(row(IDX_GAZETTEER_URI_CORRECTED)) 
                                else 
                                  None
                                
        // Resolve place (and fixed place, if any)
        val place = gazetteerURI.map(uri => index.getPlace(uri)).flatten
        val placeFixed = gazetteerURIFixed.map(uri => index.getPlace(uri)).flatten
        
        // Build JSON response object
        Json.obj(
          "toponym" -> toponym,
          "place" -> place.map(place => Json.obj(
            "uri" -> place.uri,
            "title" -> place.title,
            "names" -> place.names.map(_.labels).flatten.map(_.label).mkString(", "),
            "coordinate" -> place.getCentroid.map(coord => JsArray(Seq(JsNumber(coord.y), JsNumber(coord.x))))
          )),
          "place_fixed" -> placeFixed.map(place => Json.obj(
            "uri" -> place.uri,
            "title" -> place.title,
            "names" -> place.names.map(_.labels).flatten.map(_.label).mkString(", "),
            "coordinate" -> place.getCentroid.map(coord => JsArray(Seq(JsNumber(coord.y), JsNumber(coord.x))))
          ))
        )
      })
      
      Json.obj(
        "title" -> csv.title,
        "source" -> csv.sourceURI,
        "places" -> JsArray(places)
      )
    })
    
    val json = Json.obj("parts" -> parts)
    Json.prettyPrint(json)
  }

}