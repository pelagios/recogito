package org.pelagios.georescorrector.model

import com.vividsolutions.jts.geom.Coordinate
import org.pelagios.georescorrector.index.PlaceIndex
import org.pelagios.georescorrector.Global

class MapModel(csv: Seq[CSVModel]) {
  
  val index = Global.index
  
  private val IDX_TOPONYM = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("toponym"))
  private val IDX_GAZETTEER_URI = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("gazetteer uri"))
  private val IDX_GAZETTEER_URI_CORRECTED = csv(0).colHeadings.indexWhere(_.equalsIgnoreCase("gazetteer uri (verified/corrected)"))
  
  def toJSON: String = {  
    val jsonArray = csv.map(csv => {
      val jsonPlaces = csv.rows.map(row => {
        val toponym = if (IDX_TOPONYM > 0 && row(IDX_TOPONYM) != null) Some(row(IDX_TOPONYM)) else None
        val gazetteerURI = if (IDX_GAZETTEER_URI > 0 && row(IDX_GAZETTEER_URI) != null) Some(row(IDX_GAZETTEER_URI)) else None 
        val gazetteerURICorrected = if (IDX_GAZETTEER_URI_CORRECTED > 0 && row(IDX_GAZETTEER_URI_CORRECTED) != null) 
                                      Some(row(IDX_GAZETTEER_URI_CORRECTED)) 
                                    else 
                                      None
                                      
        val place = gazetteerURI.map(uri => index.getPlace(uri)).flatten
        val coordinate = place.map(_.getCentroid).flatten
        val placeCorrected = gazetteerURICorrected.map(uri => index.getPlace(uri)).flatten
        val coordinateCorrected = placeCorrected.map(_.getCentroid).flatten
      
        val jsonObj = Seq(
            toponym.map(t => "\"toponym\":\"" + t + "\""), 
            gazetteerURI.map(g => "\"gazetteer_uri\":\"" + g + "\""),
            place.map(place => "\"gazetteer_title\":\"" + place.title + "\""),
            place.map(place => "\"gazetteer_names\":\"" + place.names.map(_.labels).flatten.map(_.label).mkString(", ") + "\""),
            coordinate.map(coord => "\"coordinate\":[" + coord.y + "," + coord.x + "]"),
            placeCorrected.map(place => "\"gazetteer_uri_fixed\":\"" + place.uri + "\""),
            placeCorrected.map(place => "\"gazetteer_title_fixed\":\"" + place.title + "\""),
            coordinateCorrected.map(coord => "\"coordinate_fixed\":[" + coord.y + "," + coord.x + "]"))
        .filter(_.isDefined).map(_.get)
      
        "      { " + jsonObj.mkString(", ") + " }"
      }).mkString(",\n")
      
      "{\n    \"title\": \"" + csv.title.getOrElse("No Title") + "\",\n" +
      "    \"source\": \"" + csv.sourceURI.getOrElse("No URI") + "\",\n" + 
      "    \"places\": [\n" + jsonPlaces + "\n    ]\n  }"
    })
    
    "{\n" +
    "  \"parts\": [" +
    jsonArray.mkString(",") + 
    "]\n" +
    "}"
  }

}