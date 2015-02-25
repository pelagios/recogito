package global

import com.vividsolutions.jts.geom.{ Geometry, Polygon }
import index.{ IndexedPlace, IndexedPlaceNetwork, PlaceIndex }

// TODO refactor - this can now go into the PlaceIndex class
object CrossGazetteerUtils {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  
  def getConflatedPlace(uri: String): Option[(IndexedPlace, Option[Geometry])] =
    Global.index.findNetworkByPlaceURI(uri).map(network => {
      val preferredPlace = network.places.filter(_.uri == PlaceIndex.normalizeURI(uri)).head
      (preferredPlace, getPreferredGeometry(preferredPlace, network))
    })
  
  def getPreferredGeometry(place: IndexedPlace, network: IndexedPlaceNetwork): Option[Geometry] = {
    val geometry = {   
      // We use DARE geometry if available...
      val dareEquivalent = network.places.filter(_.uri.startsWith(DARE_PREFIX))
      if (dareEquivalent.size > 0) {
        dareEquivalent(0).geometry
      } else {
        // ...or the place's own geometry as a second option...
        if (place.geometry.isDefined) {
          place.geometry
        } else {
          // ...or ANY geometry from the network if neither DARE nor the original place have one
          network.places.flatMap(_.geometry).headOption
        }
      }
    }
      
    // Hack to get rid of Barrington gridsquares
    // TODO make 'collapse rectangular regions to centroid' a configurable option in app.conf
    geometry.map(g => g match {
      case p: Polygon if (p.getCoordinates.size == 5) => p.getCentroid
      case g => g
    })
  }

}