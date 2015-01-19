package global

import org.pelagios.gazetteer.GazetteerUtils
import org.pelagios.api.gazetteer.Place
import com.vividsolutions.jts.geom.Coordinate

/**
 * A temporary work around until cross-gazetteer search is fully integrated with
 * the scalagios-gazetteer library
 */
object CrossGazetteerUtils {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
  
  def getPlace(uri: String): Option[(Place, Option[Coordinate])] = {
    val normalized = GazetteerUtils.normalizeURI(uri)
    val place = Global.index.findByURI(normalized)
    
    if (place.isEmpty) {
      None
    } else {
      // We use DARE coordinates if we have them
      val coordinate = place.map(place => {
        val network = Global.index.getNetwork(place).places
        val dareEquivalent = network.filter(_.uri.startsWith(DARE_PREFIX))
        if (dareEquivalent.size > 0) {
          dareEquivalent(0).getCentroid
        } else {
          if (place.getCentroid.isDefined) {
            place.getCentroid
          } else {
            // Or ANY in the network if neither DARE nor the original place has one
            network.flatMap(_.getCentroid).headOption
          }
        }
      }).flatten
    
      Some((place.get, coordinate))
    }
  }

}