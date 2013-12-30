package org.pelagios.grct

import models._
import java.io.File
import org.openrdf.rio.RDFFormat
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.DB
import play.api.Play.current
import scala.slick.session.Database
import play.api.db.slick.Config.driver.simple._
import scala.slick.jdbc.meta.MTable
import org.pelagios.grct.io.CSVParser
import scala.io.Source
import org.pelagios.gazetteer.PlaceIndex
import org.pelagios.Scalagios
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import org.pelagios.grct.io.CSVParser

/** Play Global object **/
object Global extends GlobalSettings {

  import Database.threadLocalSession
  
  private val DATA_PLEIADES = "gazetteer/pleiades-20120826-migrated.ttl.gz"
  private val DATA_DARE = "gazetteer/dare-20131210.ttl.gz"
  
  private val INDEX_DIR = "index"
 
  lazy val index = {
    val idx = PlaceIndex.open(INDEX_DIR)
    if (idx.isEmpty) {
      Logger.info("Building new index")
      
      Logger.info("Loading Pleiades data")
      val pleiades = Scalagios.parseGazetteer(new GZIPInputStream(new FileInputStream(DATA_PLEIADES)), "http://pleiades.stoa.org/", RDFFormat.TURTLE)
      Logger.info("Inserting Pleiades into index")
      idx.addPlaces(pleiades)
    
      Logger.info("Loading DARE data")
      val dare = Scalagios.parseGazetteer(new GZIPInputStream(new FileInputStream(DATA_DARE)), "http://imperium.ahlfeldt.se/", RDFFormat.TURTLE)
      Logger.info("Inserting DARE into index") 
      idx.addPlaces(dare)
      
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
      }
       
      if (MTable.getTables("gdocuments").list().isEmpty) {
        GeoDocuments.ddl.create
      }
      
      if (MTable.getTables("gdocument_parts").list().isEmpty) {
        GeoDocumentParts.ddl.create
      }
      
      if (MTable.getTables("gdocument_texts").list().isEmpty) {
        GeoDocumentTexts.ddl.create
      }
      
      if (MTable.getTables("annotations").list().isEmpty) {
        Annotations.ddl.create
      }
      
      if (MTable.getTables("edit_history").list().isEmpty) {
        EditHistory.ddl.create        
      }
    }
  }  

}