package org.pelagios.georescorrector

import java.util.Properties
import java.io.{ File, FileReader }
import org.openrdf.rio.RDFFormat
import org.pelagios.georescorrector.index.PlaceIndex
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.io.PrintWriter

/** A Play-like Global object.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object Global {
  
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  private val INDEX_DATAFILE = "gazetteer/pleiades-20120826-migrated.ttl.gz"
  
  private val INDEX_DIR = new File("index")
  
  private val props = new Properties()
  props.load(new FileReader("app.properties"))
  
  lazy val username = props.getProperty("username")
  
  lazy val password = props.getProperty("password")
  
  val index = {
    if (INDEX_DIR.exists()) {
      logger.info("Loading existing index")
      new PlaceIndex(INDEX_DIR)
    } else {
      logger.info("Building new index... ")
      val idx = PlaceIndex.initIndex(INDEX_DIR, "http://pelagios.org/dummy-gazetteer", INDEX_DATAFILE, RDFFormat.TURTLE)
      logger.info(" done.")
      idx
    }
  }
 
  def writePIDToFile(file: String) = {
    val pid = ManagementFactory.getRuntimeMXBean().getName()  
    val writer = new PrintWriter(new File(file))
    writer.println(pid)
    writer.flush()
    writer.close()
  }
  
}