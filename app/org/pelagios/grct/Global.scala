package org.pelagios.grct

import java.io.File
import play.api.{ Application, GlobalSettings }
import play.api.Logger
import org.pelagios.grct.index.PlaceIndex
import org.openrdf.rio.RDFFormat

object Global extends GlobalSettings {
  
  private val INDEX_DATAFILE = "gazetteer/pleiades-20120826-migrated.ttl.gz"
  
  private val INDEX_DIR = new File("index")
  
  lazy val index = {
    if (INDEX_DIR.exists()) {
      Logger.info("Loading existing index")
      new PlaceIndex(INDEX_DIR)
    } else {
      Logger.info("Building new index... ")
      val idx = PlaceIndex.initIndex(INDEX_DIR, "http://pelagios.org/dummy-gazetteer", INDEX_DATAFILE, RDFFormat.TURTLE)
      Logger.info(" done.")
      idx
    }
  }
  
  override def onStart(app: Application): Unit = {
    
  }  

}