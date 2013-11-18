package org.pelagios.georescorrector.index

import java.io.File

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.openrdf.rio.RDFFormat
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class PlaceIndexTest extends FunSuite with BeforeAndAfter {

  val INDEX_DIR = new File("tmp")
    
  val DATA_FILE = "gazetteer/pleiades-20120826-migrated.ttl.gz"
  
  before {
    FileUtils.deleteDirectory(INDEX_DIR)
  }
    
  test("Initialize Place Index") {
    val index = PlaceIndex.initIndex(INDEX_DIR, "http://my.gazetteer.org/", DATA_FILE, RDFFormat.TURTLE)
  }
  
  after {
    FileUtils.deleteDirectory(INDEX_DIR)
  }
  
}