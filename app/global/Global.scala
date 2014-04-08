package global

import akka.actor.Cancellable
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

/** Play Global object **/
object Global extends GlobalSettings {

  import Database.threadLocalSession
  
  private val GAZETTEER_DIR = "gazetteer"
  
  private val INDEX_DIR = "index"
    
  private var statsDemon: Option[Cancellable] = None
 
  lazy val index = {
    val idx = PlaceIndex.open(INDEX_DIR)
    if (idx.isEmpty) {
      Logger.info("Building new index")
      
      val dumps = Play.current.configuration.getString("recogito.gazetteers")
        .map(_.split(",").toSeq).getOrElse(Seq.empty[String]).map(_.trim)
        
      dumps.foreach(f => {
        Logger.info("Loading gazetteer dump: " + f)
        val is = if (f.endsWith(".gz"))
            new GZIPInputStream(new FileInputStream(new File(GAZETTEER_DIR, f)))
          else
            new FileInputStream(new File(GAZETTEER_DIR, f))
        
        val places = Scalagios.readPlaces(is, "http://pelagios.org/", RDFFormat.TURTLE).toSeq
        val names = places.flatMap(_.names)
        Logger.info("Inserting " + places.size + " places with " + names.size + " names into index")
        idx.addPlaces(places)
      })
      
      Logger.info("Index complete")      
    }
    idx
  }

  lazy val database = Database.forDataSource(DB.getDataSource()) 

  override def onStart(app: Application): Unit = {
    // Create DB tables if they don't exist
    database.withSession {
      if (MTable.getTables("users").list().isEmpty) {
        Users.ddl.create
         
        // Create default admin user        
        val salt = User.randomSalt
        Users.insert(User("admin", User.computeHash(salt + "admin"), salt, "*", true))
      }
       
      if (MTable.getTables("gdocuments").list().isEmpty)
        GeoDocuments.ddl.create
      
      if (MTable.getTables("gdocument_parts").list().isEmpty)
        GeoDocumentParts.ddl.create
      
      if (MTable.getTables("gdocument_texts").list().isEmpty)
        GeoDocumentTexts.ddl.create
        
      if (MTable.getTables("collection_memberships").list().isEmpty)
        CollectionMemberships.ddl.create
      
      if (MTable.getTables("annotations").list().isEmpty)
        Annotations.ddl.create
      
      if (MTable.getTables("edit_history").list().isEmpty)
        EditHistory.ddl.create
        
      if (MTable.getTables("stats_history").list().isEmpty)
        StatsHistory.ddl.create
    }
    
    // Periodic stats logging
    val runStatsDemon = Play.current.configuration.getBoolean("recogito.stats.enabled").getOrElse(false)
    if (runStatsDemon) {
      Logger.info("Starting stats logging background actor")
      statsDemon = Some(StatsDemon.start())
    }
  }  
  
  override def onStop(app: Application): Unit = {
    if (statsDemon.isDefined) {
      Logger.info("Shutting down stats logging background actor")
      statsDemon.get.cancel
      statsDemon = None
    }
  }

}
