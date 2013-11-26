package org.pelagios.grct

import models._
import java.io.File
import org.openrdf.rio.RDFFormat
import org.pelagios.grct.index.PlaceIndex
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.DB
import play.api.Play.current
import scala.slick.session.Database
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

/** Play Global object **/
object Global extends GlobalSettings {

  import Database.threadLocalSession
  
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

  lazy val database = Database.forDataSource(DB.getDataSource()) 

  override def onStart(app: Application): Unit = {
    // Create DB tables if they don't exist
    database.withSession {
      if (MTable.getTables("user").list().isEmpty) {
        Users.ddl.create
      
        // Dummy data (temporary)
        Users.insertAll(
          User(Some(1), "pelagios", "pelagios"),
          User(Some(2), "admin", "admin"))
      }
    }
  }  

}