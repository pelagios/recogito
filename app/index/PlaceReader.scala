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

trait PlaceReader extends PlaceIndexBase {
  
  private val INVALID_QUERY_CHARS = Seq("(", ")", "[", "]")
  
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
        outerQuery.add(new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(expandedQuery), BooleanClause.Occur.MUST)
        
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
        new MultiFieldQueryParser(Version.LUCENE_4_9, fields, analyzer).parse(expandedQuery)
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
    
    val point = spatialCtx.makePoint(lat, lon)
    val args = new SpatialArgs(SpatialOperation.IsWithin, spatialCtx.makeCircle(lon, lat, DistanceUtils.dist2Degrees(100, DistanceUtils.EARTH_MEAN_RADIUS_KM)))
    val filter = spatialStrategy.makeFilter(args)
    
    val valueSource = spatialStrategy.makeDistanceValueSource(point)
    val sortField = valueSource.getSortField(false)    
    val distanceSort = new Sort(sortField).rewrite(searcher)
    
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
      
      /*
      scoreDocs.foreach(scoreDoc => {
        val doc = searcher.doc(scoreDoc.doc)
        val docPoint = spatialCtx.readShape(doc.get(spatialStrategy.getFieldName())).asInstanceOf[Point]
        val distance = spatialCtx.getDistCalc().distance(args.getShape.getCenter, docPoint)
        val distanceKM = DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM)
        Logger.info("distance: " + distanceKM)
      })
      */
      
      scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
    } finally {
      placeSearcherManager.release(searcher)
    }
  }
  
}