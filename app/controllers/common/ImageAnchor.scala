package controllers.common

import play.api.libs.json.{ Json, JsValue }
import play.api.Logger

/** Wrapper around image annotation anchor JSON.
  *  
  * This class strictly expects well-formed image anchors. Invalid JSON will
  * result in a runtime exception.
  */
class ImageAnchor(anchor: String) {
  
  Logger.info(anchor)
  
  private val json = Json.parse(anchor)
  
  if ((json \ "type").as[String] != "toponym")
    throw new RuntimeException("Image anchor not well-formed: " + anchor)
  
  private val geometry = (json \ "geometry").as[JsValue]
   
  val x = (geometry \ "x").as[Double]
    
  val y = (geometry \ "y").as[Double]
    
  // For future use
  lazy val a = (geometry \ "a").as[Double]
    
  lazy val l = (geometry \ "l").as[Double]
    
  lazy val h = (geometry \ "h").as[Double] 
    
}