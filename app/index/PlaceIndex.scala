package index

import com.spatial4j.core.context.jts.JtsSpatialContext
import java.io.File
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.search.{ SearcherManager, SearcherFactory }
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import play.api.Logger

private[index] class PlaceIndexBase(indexDir: File) {
  
  private val spatialCtx = JtsSpatialContext.GEO
  
  private val maxLevels = 11 //results in sub-meter precision for geohash
  
  private val spatialStrategy =
    new RecursivePrefixTreeStrategy(new GeohashPrefixTree(spatialCtx, maxLevels), Fields.GEOMETRY)
  
  private val index = FSDirectory.open(indexDir)
    
  protected val analyzer = new StandardAnalyzer(Version.LUCENE_4_9)
  
  protected val placeSearcherManager = new SearcherManager(index, new SearcherFactory())
      
  protected lazy val placeWriter: IndexWriter = 
    new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_4_9, analyzer))
  
  def refresh() = {
    Logger.info("Refreshing index readers")
    placeWriter.commit()
    placeSearcherManager.maybeRefresh()
  }
  
  def close() = {
    analyzer.close()    
    placeWriter.close()    
    placeSearcherManager.close()    
    index.close()
  }
      
}

class PlaceIndex private(indexDir: File) extends PlaceIndexBase(indexDir) with PlaceReader with PlaceWriter
  
object PlaceIndex {
  
  def open(indexDir: String) = new PlaceIndex(createIfNotExists(new File(indexDir)))
  
  private def createIfNotExists(dir: File): File = {
    if (!dir.exists) {
      dir.mkdirs()  
      val initConfig = new IndexWriterConfig(Version.LUCENE_4_9, new StandardAnalyzer(Version.LUCENE_4_9))
      val initializer = new IndexWriter(FSDirectory.open(dir), initConfig)
      initializer.close()      
    }
    
    dir  
  }
  
  def normalizeURI(uri: String) = {
    val noFragment = if (uri.indexOf('#') > -1) uri.substring(0, uri.indexOf('#')) else uri
    if (noFragment.endsWith("/"))
      noFragment.substring(0, noFragment.size - 1)
    else 
      noFragment
  }
  
}
