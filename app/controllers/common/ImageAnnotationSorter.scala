package controllers.common

import models.Annotation

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
        if (toSort.size > 1) {
          val (start, rest) = selectStart(toSort)
          val (nearestNeighbour, unvisited) = getNearest(start, rest)
          sortByNearestNeighbour(unvisited, Seq(start, nearestNeighbour))
        } else {
          // Trivial case - there is only one annotation
          toSort
        }
      } else {
        // We pick the last sorted annotation and continue from there
        val (nearestNeighbour, remainder) = getNearest(sorted.last, toSort)
        sortByNearestNeighbour(remainder, sorted :+ nearestNeighbour)
      }
    }
  }
    
}