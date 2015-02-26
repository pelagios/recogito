package index

import net.sf.junidecode.Junidecode
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, PrefixQuery, ScoreDoc, Sort, TermQuery, TopScoreDocCollector }
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.util.Version
import com.spatial4j.core.distance.DistanceUtils
import org.apache.lucene.search.MatchAllDocsQuery
import play.api.Logger
import com.spatial4j.core.shape.Point

trait PlaceReader extends PlaceIndexBase {
  
  // Characters to remove from search queries
  private val INVALID_QUERY_CHARS = Seq("(", ")", "[", "]")
  
  // Maximum radius for spatial proximity search
  private val PROXIMITY_SEARCH_RADIUS_KM = 500
  
  def isEmpty: Boolean = (numPlaceNetworks == 0)
  
  def numPlaceNetworks: Int = {
    val searcher = placeSearcherManager.acquire()
    try {
      searcher.getIndexReader().numDocs()
    } finally {
      placeSearcherManager.release(searcher)
    }
  }
  
  def findPlaceByURI(uri: String): Option[IndexedPlace] =
    findNetworkByPlaceURI(uri).flatMap(_.getPlace(uri))
    
  def findNetworkByPlaceURI(uri: String): Option[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(Fields.URI, PlaceIndex.normalizeURI(uri)))

    val searcher = placeSearcherManager.acquire()
    val collector = TopScoreDocCollector.create(1, true)
    try {
      searcher.search(q, collector)
      collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).headOption
    } finally {
      placeSearcherManager.release(searcher)
    }
  }

  def findNetworkByCloseMatch(uri: String): Seq[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(Fields.PLACE_MATCH, PlaceIndex.normalizeURI(uri)))
    
    val searcher = placeSearcherManager.acquire()
    val numHits = Math.max(1, numPlaceNetworks) // Has to be minimum 1, but can never exceed size of index
    val collector = TopScoreDocCollector.create(numHits, true)
    try {
      searcher.search(q, collector)
      collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc)))
    } finally {
      placeSearcherManager.release(searcher)
    }
  }
  
  def search(query: String, limit: Int, offset: Int, allowedPrefixes: Option[Seq[String]] = None): Seq[IndexedPlaceNetwork] = {
    // We only support keyword queries, and remove all special characters that may mess it up
    val normalizedQuery = INVALID_QUERY_CHARS
      .foldLeft(query)((normalized, invalidChar) => normalized.replace(invalidChar, ""))
      
    val transliteratedQuery = Junidecode.unidecode(normalizedQuery)    
    
    val expandedQuery =
      if (normalizedQuery == transliteratedQuery) 
        normalizedQuery
      else
        normalizedQuery + " OR " + transliteratedQuery
    
    val fields = Seq(Fields.TITLE, Fields.DESCRIPTION, Fields.PLACE_NAME).toArray       
    val q = 
      if (allowedPrefixes.isDefined) {
        val outerQuery = new BooleanQuery()
        outerQuery.add(new MultiFieldQueryParser(Version.LATEST, fields, analyzer).parse(expandedQuery), BooleanClause.Occur.MUST)
        
        if (allowedPrefixes.get.size == 1) {
          outerQuery.add(new PrefixQuery(new Term(Fields.URI, allowedPrefixes.get.head)), BooleanClause.Occur.MUST)    
        } else {
          val innerQuery = new BooleanQuery()
          allowedPrefixes.get.foreach(prefix => 
            innerQuery.add(new PrefixQuery(new Term(Fields.URI, prefix)), BooleanClause.Occur.SHOULD))
          outerQuery.add(innerQuery, BooleanClause.Occur.MUST)
        }
        
        outerQuery
      } else {
        // Just a plain text query
        new MultiFieldQueryParser(Version.LATEST, fields, analyzer).parse(expandedQuery)
      }
    
    val searcher = placeSearcherManager.acquire()
    val collector = TopScoreDocCollector.create(offset + limit, true)
    try {
      searcher.search(q, collector)
      collector.topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
    } finally {
      placeSearcherManager.release(searcher)
    }  
  }
  
  def findNearby(lat: Double, lon: Double, limit: Int, allowedPrefixes: Option[Seq[String]] = None): Seq[IndexedPlaceNetwork] = {
    val searcher = placeSearcherManager.acquire()
    
    val point = PlaceIndex.ctx.makePoint(lon, lat)
    val args = new SpatialArgs(SpatialOperation.IsWithin, 
        PlaceIndex.ctx.makeCircle(point, DistanceUtils.dist2Degrees(PROXIMITY_SEARCH_RADIUS_KM, DistanceUtils.EARTH_MEAN_RADIUS_KM)))
    val filter = PlaceIndex.strategy.makeFilter(args)
    
    val valueSource = PlaceIndex.strategy.makeDistanceValueSource(point)
    val distanceSort = new Sort(valueSource.getSortField(false)).rewrite(searcher)
    
    val query =
      if (allowedPrefixes.isDefined) {
        if (allowedPrefixes.get.size == 1) {
          new PrefixQuery(new Term(Fields.URI, allowedPrefixes.get.head))    
        } else {
          val q = new BooleanQuery()
          allowedPrefixes.get.foreach(prefix => 
            q.add(new PrefixQuery(new Term(Fields.URI, prefix)), BooleanClause.Occur.SHOULD))
          q
        }
      } else {
        new MatchAllDocsQuery()
      }
    
    try {  
      val topDocs = searcher.search(query, filter, limit, distanceSort) 
      val scoreDocs = topDocs.scoreDocs  
      scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
    } finally {
      placeSearcherManager.release(searcher)
    }
  }
  
}