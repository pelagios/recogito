package controllers.common

import models.Annotation
import play.api.libs.json.{ Json, JsValue }
import play.api.Logger

/** Wrapper around image annotation anchor JSON.
  *  
  * This class strictly expects well-formed image anchors. Invalid JSON will
  * result in a runtime exception.
  */
private[common] class ImageAnchor(anchor: String) {
  
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

/** Helper class to sort image annotations by useful criteria **/
object ImageAnnotationSorter {
    
  private def getXY(annotation: Annotation): (Double, Double) = {
    val json = if (annotation.correctedAnchor.isDefined) annotation.correctedAnchor else annotation.anchor
    if (json.isDefined) {
      val anchor = new ImageAnchor(json.get)
      (anchor.x, anchor.y)
    } else {
      // Should never happen - we place the annotation into infinity in this case
      (Double.MaxValue, Double.MaxValue)
    }
  }
  
  private def selectStart(annotations: Seq[Annotation]): (Annotation, Seq[Annotation]) = {
    val vecNormsSq = annotations.map(a => {
      val xy = getXY(a)
      (a, Math.pow(xy._1, 2) + Math.pow(xy._2, 2))
    }).sortBy(_._2)
    
    (vecNormsSq.head._1, vecNormsSq.tail.map(_._1))
  }
    
  private def getNearest(annotation: Annotation, unvisited: Seq[Annotation]): (Annotation, Seq[Annotation]) = {    
    val thisXY = getXY(annotation)    
    val distancesSq = unvisited.map(neighbour => {
      val thatXY = getXY(neighbour)
      val dx = thisXY._1 - thatXY._1
      val dy = thisXY._2 - thatXY._2
      (neighbour, Math.pow(dx, 2) + Math.pow(dy, 2))
    }).sortBy(_._2)
    
    (distancesSq.head._1, distancesSq.tail.map(_._1))
  }

  def sortByNearestNeighbour(toSort: Seq[Annotation], sorted: Seq[Annotation] = Seq.empty[Annotation]): Seq[Annotation] = {
    if (toSort.isEmpty) {
      sorted // We're done
    } else {
      if (sorted.isEmpty) {
        // We don't have any sorted annotations yet, so we pick (by convention) the top/left-most one as a start
        val (start, rest) = selectStart(toSort)
        val (nearestNeighbour, unvisited) = getNearest(start, rest)
        sortByNearestNeighbour(unvisited, Seq(start, nearestNeighbour))
      } else {
        // We pick the last sorted annotation and continue from there
        val (nearestNeighbour, remainder) = getNearest(sorted.last, toSort)
        sortByNearestNeighbour(remainder, sorted :+ nearestNeighbour)
      }
    }
  }
    
}