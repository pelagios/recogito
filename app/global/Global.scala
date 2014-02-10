package global

import models._
import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream
import org.openrdf.rio.RDFFormat
import org.pelagios.Scalagios
import org.pelagios.gazetteer.PlaceIndex
import play.api.Play
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.DB
import play.api.db.slick.Config.driver.simple._
import scala.slick.session.Database
import scala.slick.jdbc.meta.MTable
import akka.actor.Cancellable

/** Play Global object **/
object Global extends GlobalSettings {

  import Database.threadLocalSession
  
  private val DATA_PLEIADES = "gazetteer/pleiades-20120826-migrated.ttl.gz"
  private val DATA_DARE = "gazetteer/dare-20131210.ttl.gz"
  
  private val INDEX_DIR = "index"
  
  private val RUN_STATS_DEMON = Play.current.configuration.getBoolean("recogito.stats.enabled").getOrElse(false)
  private val statsDemon = if (RUN_STATS_DEMON) Some(new StatsDemon()) else None
 
  lazy val index = {
    val idx = PlaceIndex.open(INDEX_DIR)
    if (idx.isEmpty) {
      Logger.info("Building new index")
      
      Logger.info("Loading Pleiades data")
      val pleiades = Scalagios.readPlaces(new GZIPInputStream(new FileInputStream(DATA_PLEIADES)), "http://pleiades.stoa.org/", RDFFormat.TURTLE)
      Logger.info("Inserting " + pleiades.size + " Pleiades places into index")
      idx.addPlaces(pleiades)
    
      Logger.info("Loading DARE data")
      val dare = Scalagios.readPlaces(new GZIPInputStream(new FileInputStream(DATA_DARE)), "http://imperium.ahlfeldt.se/", RDFFormat.TURTLE)
      Logger.info("Inserting " + dare.size +" DARE places into index") 
      idx.addPlaces(dare)
      
      Logger.info("Index complete")      
    }
    idx
  }

  lazy val database = Database.forDataSource(DB.getDataSource()) 

  override def onStart(app: Application): Unit = {
    // Create DB tables if they don't exist
    database.withSession {
      if (MTable.getTables("users").list().isEmpty)
        Users.ddl.create
       
      if (MTable.getTables("gdocuments").list().isEmpty)
        GeoDocuments.ddl.create
      
      if (MTable.getTables("gdocument_parts").list().isEmpty)
        GeoDocumentParts.ddl.create
      
      if (MTable.getTables("gdocument_texts").list().isEmpty)
        GeoDocumentTexts.ddl.create
      
      if (MTable.getTables("annotations").list().isEmpty)
        Annotations.ddl.create
      
      if (MTable.getTables("edit_history").list().isEmpty)
        EditHistory.ddl.create
        
      if (MTable.getTables("stats_history").list().isEmpty)
        StatsHistory.ddl.create
    }
    
    // Periodic stats logging
    if (statsDemon.isDefined) {
      Logger.info("Starting stats logging background actor")
      statsDemon.get.start()
    }
  }  
  
  override def onStop(app: Application): Unit = {
    if (statsDemon.isDefined) {
      Logger.info("Shutting down stats logging background actor")
      statsDemon.get.dispose()
    }
  }

}