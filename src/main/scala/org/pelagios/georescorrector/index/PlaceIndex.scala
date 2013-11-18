package org.pelagios.georescorrector.index

import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream
import com.vividsolutions.jts.io.WKTWriter
import org.apache.lucene.util.Version
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.document.{ Document, Field, StringField, TextField }
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{ IndexReader, IndexWriter, IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, IndexSearcher, TermQuery, TopScoreDocCollector }
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios
import org.pelagios.api._
import org.apache.commons.io.FileUtils
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser

class PlaceIndex(directory: File) {
  
  import PlaceIndex._
  
  if (!directory.exists()) directory.mkdirs()
  
  private val index = FSDirectory.open(directory)
  
  private val analyzer = new StandardAnalyzer(Version.LUCENE_44)
  
  private def normalizeURI(uri: String): String = {
    if (uri.endsWith("#this"))
      uri
    else
      if (uri.endsWith("/")) 
        uri.substring(0, uri.length - 1) + "#this"
      else
        uri + "#this"    
  }
  
  def addPlaces(places: Iterable[Place]) = {
    val writer = new IndexWriter(index, new IndexWriterConfig(Version.LUCENE_44, analyzer))
    val wktWriter = new WKTWriter() 

    places.foreach(place => {     
      val doc = new Document()
      doc.add(new StringField(FIELD_URI, normalizeURI(place.uri), Field.Store.YES))
      doc.add(new TextField(FIELD_TITLE, place.title, Field.Store.YES))
      place.descriptions.foreach(description => doc.add(new TextField(FIELD_DESCRIPTION, description.label, Field.Store.YES)))
      place.names.foreach(name => {
        name.labels.foreach(label => {
          doc.add(new TextField(FIELD_NAME_LABEL, label.label, Field.Store.YES)) 
        })
        name.altLabels.foreach(altLabel => doc.add(new TextField(FIELD_NAME_ALTLABEL, altLabel.label, Field.Store.YES)))
      })
      
      place.locations.foreach(location => doc.add(new StringField(FIELD_GEOMETRY, wktWriter.write(location.geometry), Field.Store.YES)))
      writer.addDocument(doc)
    })

    writer.close
    index    
  }
  
  def getPlace(uri: String): Option[Place] = {
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(FIELD_URI, normalizeURI(uri))), BooleanClause.Occur.MUST)
    
    val reader = IndexReader.open(index)
    val searcher = new IndexSearcher(reader)
    
    val collector = TopScoreDocCollector.create(1, true)
    searcher.search(q, collector)
    
    val places = collector.topDocs.scoreDocs.map(scoreDoc => {
      val doc = searcher.doc(scoreDoc.doc)
      Place(doc.get(FIELD_URI), doc.get(FIELD_TITLE),
            names = doc.getValues(FIELD_NAME_LABEL).map(label => Name(Label(label))).toSeq,
            locations = doc.getValues(FIELD_GEOMETRY).map(wkt => Location(Location.parseWKT(wkt))).toSeq)
    })
    
    if (places.size > 0)
      return Some(places(0))
    else
      None
  }
  
  def query(query: String): Seq[Place] = {
    val fields = Seq(FIELD_TITLE, FIELD_NAME_LABEL, FIELD_NAME_ALTLABEL).toArray    
    val q = new MultiFieldQueryParser(Version.LUCENE_44, fields, analyzer).parse(query + "~")
    
    val reader = IndexReader.open(index)
    val searcher = new IndexSearcher(reader)
    
    val collector = TopScoreDocCollector.create(20, true)
    searcher.search(q, collector)
    
    collector.topDocs.scoreDocs.map(scoreDoc => {
      val doc = searcher.doc(scoreDoc.doc)
      Place(doc.get(FIELD_URI), doc.get(FIELD_TITLE),
            names = doc.getValues(FIELD_NAME_LABEL).map(label => Name(Label(label))).toSeq,
            locations = doc.getValues(FIELD_GEOMETRY).map(wkt => Location(Location.parseWKT(wkt))).toSeq)
    })
  }
  
}

object PlaceIndex {
  
  private[index] val FIELD_URI = "uri"
  private[index] val FIELD_TITLE = "title"
  private[index] val FIELD_DESCRIPTION = "description"
  private[index] val FIELD_NAME_LABEL = "label"
  private[index] val FIELD_NAME_ALTLABEL = "alt-label"
  private[index] val FIELD_GEOMETRY = "wkt" 
    
  def initIndex(directory: File, baseURI: String, datafile: String, format: RDFFormat) = {
    if (directory.exists())
      FileUtils.deleteDirectory(directory)
    
    val is = if (datafile.endsWith(".gz")) {
               new GZIPInputStream(new FileInputStream(datafile))     
             } else {
               new FileInputStream(datafile)
             }
    
    val places = Scalagios.parseGazetteer(is, baseURI, format)
    
    
    val index = new PlaceIndex(directory)
    index.addPlaces(places)
    index
  }  
  
}