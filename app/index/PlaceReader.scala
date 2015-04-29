package index

import com.spatial4j.core.distance.DistanceUtils
import net.sf.junidecode.Junidecode
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, MatchAllDocsQuery, PrefixQuery, ScoreDoc, Sort, TermQuery, TopScoreDocCollector }
import org.apache.lucene.spatial.query.{ SpatialArgs, SpatialOperation }
import org.apache.lucene.util.Version

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
     
    // In general, we want to transliterate - but NOT for Chinese! It would turn the query '張掖郡' 
    // to '張掖郡 OR Zhang Yi Jun' - and that will lead to lots of false matches 
    val expandedQuery = {
      val cjk = "\\p{script=Han}".r
      cjk.findFirstIn(query) match {
        
        // CJK character - don't transliterate
        case Some(_) =>  normalizedQuery
        
        // Non-CJK query - transliterate
        case None => {
          val transliteratedQuery = Junidecode.unidecode(normalizedQuery)    
          if (normalizedQuery == transliteratedQuery) 
            normalizedQuery
          else
           normalizedQuery + " OR " + transliteratedQuery
        }
      }      
    }
    
    val fields = Seq(Fields.TITLE, Fields.DESCRIPTION, Fields.PLACE_NAME).toArray     
    val queryParser = new MultiFieldQueryParser(Version.LATEST, fields, analyzer)
    queryParser.setAutoGeneratePhraseQueries(true)
    
    val q = 
      if (allowedPrefixes.isDefined) {
        val outerQuery = new BooleanQuery()
        outerQuery.add(queryParser.parse(expandedQuery), BooleanClause.Occur.MUST)
        
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
        queryParser.parse(expandedQuery)
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