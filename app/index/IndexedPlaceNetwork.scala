package index

import com.spatial4j.core.context.jts.JtsSpatialContext
import org.apache.lucene.document.{ Document, Field, StringField, StoredField, TextField }
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import play.api.Logger

case class NetworkNode(uri: String, place: Option[IndexedPlace], isInnerNode: Boolean)

case class NetworkEdge(source: Int, target: Int, isInnerEdge: Boolean)

/** Represents a network of gazetteer records, interlinked with closeMatch statements.
  *  
  * A network is represented as a single document in the index. The 'sub'-places to the network
  * are not individually indexed (because we don't want them to show up as individual search 
  * results). Instead, they are stored as serialized JSON.  
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
class IndexedPlaceNetwork private[index] (private[index] val doc: Document) {
  
  /** The first place URI added to the network **/
  val seedURI: String = doc.get(Fields.URI)

  /** The network title - identical to the title of the place that started the network **/
  val title: String = doc.get(Fields.TITLE)
  
  /** The network description - identical to the first place description added to the network **/
  val description: Option[String] = Option(doc.get(Fields.DESCRIPTION))
  
  /** All indexed places in this network **/
  val places: Seq[IndexedPlace] =
    doc.getValues(Fields.PLACE_AS_JSON).toSeq
      .map(new IndexedPlace(_))
  
  /** Network nodes and edges **/
  val (nodes, edges) = {
    val links = places.flatMap(p => Seq.fill(p.matches.size)(p.uri).zip(p.matches))   
    val nodes = (links.map(_._1) ++ links.map(_._2)).distinct.map(uri => { 
      // If the node is an indexed place, it's an inner node; otherwise we require > 1 links to the node
      val place = places.find(_.uri == uri)
      val isInner = place.isDefined || links.filter(_._2 == uri).size > 1
      NetworkNode(uri, place, isInner) 
    })
    
    val edges = links.map { case (sourceURI, targetURI) => {
      val source = nodes.find(_.uri == sourceURI).get
      val target = nodes.find(_.uri == targetURI).get
      NetworkEdge(nodes.indexOf(source), nodes.indexOf(target), source.isInnerNode && target.isInnerNode) 
    }}  

    (nodes, edges)
  }

  /** Helper method to get a place with a specific URI **/
  def getPlace(uri: String): Option[IndexedPlace] =
    places.find(_.uri == PlaceIndex.normalizeURI(uri))
        
  override def equals(o: Any) = o match {
    case other: IndexedPlaceNetwork => other.seedURI == seedURI
    case _  => false
  }
  
  override def hashCode = seedURI.hashCode
    
}

object IndexedPlaceNetwork {
  
  /** Creates a new place network with a single place **/
  def createNew(): IndexedPlaceNetwork = 
    new IndexedPlaceNetwork(new Document())
  
  def join(places: Seq[IndexedPlace]) = {
    val joinedDoc = new Document() 
    places.foreach(addPlaceToDoc(_, joinedDoc))
    new IndexedPlaceNetwork(joinedDoc)
  }
  
  /** Merges the place into the network **/
  def join(place: IndexedPlace, network: IndexedPlaceNetwork): IndexedPlaceNetwork =
    join(place, Seq(network))
    
  /** Merges the place and the networks into one network **/
  def join(place: IndexedPlace, networks: Seq[IndexedPlaceNetwork]): IndexedPlaceNetwork = {
    val joinedDoc = new Document() 
    
    val allPlaces = networks.flatMap(_.places) :+ place
    allPlaces.foreach(addPlaceToDoc(_, joinedDoc))
    
    new IndexedPlaceNetwork(joinedDoc)   
  }
      
  private[index] def addPlaceToDoc(place: IndexedPlace, doc: Document): Document = {
    // Place URI
    doc.add(new StringField(Fields.URI, PlaceIndex.normalizeURI(place.uri), Field.Store.YES))

    // Title
    if (doc.get(Fields.TITLE) == null)
      // If the network is still be empty, its title is null. In this case, store the place title as network title
      doc.add(new TextField(Fields.TITLE, place.label, Field.Store.YES))
    else
      // Otherwise just index the place title, but don't store
      doc.add(new TextField(Fields.TITLE, place.label, Field.Store.NO))
      
    // Description
    if (place.description.isDefined) {
      if (doc.get(Fields.DESCRIPTION) == null)
        // If there is no stored description, store (and index) this one 
        doc.add(new TextField(Fields.DESCRIPTION, place.description.get, Field.Store.YES))
      else
        // Otherwise, just index (but don't store)
        doc.add(new TextField(Fields.DESCRIPTION, place.description.get, Field.Store.NO)) 
    }
    
    // Index (but don't store) all names
    place.names.foreach(literal => doc.add(new TextField(Fields.PLACE_NAME, literal.chars, Field.Store.NO)))
      
    // Update list of matches
    val newMatches = place.matches.map(PlaceIndex.normalizeURI(_)).distinct
    val knownMatches = doc.getValues(Fields.PLACE_MATCH).toSeq // These are distinct by definition
    newMatches.diff(knownMatches).foreach(anyMatch =>
      doc.add(new StringField(Fields.PLACE_MATCH, anyMatch, Field.Store.YES)))
          
    // Index shape geometry
    if (place.geometry.isDefined)
      try {
        val shape = PlaceIndex.ctx.makeShape(place.geometry.get)
        PlaceIndex.strategy.createIndexableFields(shape).foreach(doc.add(_))
        doc.add(new StoredField(PlaceIndex.strategy.getFieldName, PlaceIndex.ctx.toString(shape)))
      } catch {
        case _: Throwable => Logger.info("Cannot index geometry: " + place.geometry.get)
      }
    
    // Add the JSON-serialized place as a stored (but not indexed) field
    doc.add(new StoredField(Fields.PLACE_AS_JSON, place.toString))    
    
    doc
  }
  
  def buildNetworks(places: Seq[IndexedPlace]): Seq[IndexedPlaceNetwork] = {
    
    def isConnected(place: IndexedPlace, network: Seq[IndexedPlace]): Boolean = {
      val networkURIs = network.map(_.uri)
      val networkMatches = network.flatMap(_.matches)
      
      val outboundMatches = place.matches.exists(skosMatch => networkURIs.contains(skosMatch))
      val inboundMatches = networkMatches.contains(place.uri)
      val indirectMatches = place.matches.exists(skosMatch => networkMatches.contains(skosMatch))

      outboundMatches || inboundMatches || indirectMatches      
    }
    
    val networks = places.foldLeft(Seq.empty[Seq[IndexedPlace]])((networks, place) => {  
      val connectedNetworks = networks.filter(network => isConnected(place, network))
      val disconnectedNetworks = networks.diff(connectedNetworks)
      
      if (connectedNetworks.size == 0) {
        // Not connected with anything - add as a new network
        networks :+ Seq(place)
      } else {
        // Connected with one or more networks - flatten and append
        disconnectedNetworks :+ (connectedNetworks.flatten :+ place)
      }
    })
    
    networks.map(join(_))
  }
  
}
