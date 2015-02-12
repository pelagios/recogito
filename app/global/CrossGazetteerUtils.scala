package global

import org.pelagios.gazetteer.GazetteerUtils
import org.pelagios.api.gazetteer.{ Location, Place }
import com.vividsolutions.jts.geom.Polygon

/**
 * A temporary work around until cross-gazetteer search is fully integrated with
 * the scalagios-gazetteer library
 */
object CrossGazetteerUtils {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  
  def getPlace(uri: String): Option[(Place, Option[Location])] = {
    val normalized = GazetteerUtils.normalizeURI(uri)
    val place = Global.index.findByURI(normalized)
    
    if (place.isEmpty) {
      None
    } else {
      val location = place.flatMap(place => {
        val network = Global.index.getNetwork(place).places
        
        // We use DARE geometry if available
        val dareEquivalent = network.filter(_.uri.startsWith(DARE_PREFIX))
        if (dareEquivalent.size > 0) {
          dareEquivalent(0).locations.headOption
        } else {
          // Or the place's own geometry as a second option
          if (!place.locations.isEmpty) {
            place.locations.headOption
          } else {
            // Or ANY geometry from the network if neither DARE nor the original place have one
            network.flatMap(_.locations).headOption
          }
        }
      })
      
      // Horrible hack to get rid of Barrington gridsquares:
      // whenever there's a rectangle, collapse it to centroid
      val patchedLocation = location.map(l => l.geometry match {
        case p: Polygon if (p.getCoordinates.size == 5) => Location(p.getCentroid)
        case g => l
      })
      
      Some((place.get, patchedLocation))
    }
  }

}