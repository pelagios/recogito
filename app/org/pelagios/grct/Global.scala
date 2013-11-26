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
import org.pelagios.grct.importer.CSVImporter

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
      if (MTable.getTables("users").list().isEmpty) {
        Users.ddl.create
      
        // Dummy data (temporary)
        Users.insertAll(
          User(Some(0), "pelagios", "pelagios"),
          User(Some(1), "admin", "admin"))
      }
       
      if (MTable.getTables("gdocuments").list().isEmpty) {
        GeoDocuments.ddl.create
        
        GeoDocuments.insert(
          GeoDocument(Some(0), "Bordeaux Itinerary"))
      }
      
      if (MTable.getTables("gdocument_parts").list().isEmpty) {
        GeoDocumentParts.ddl.create
        
        GeoDocumentParts.insertAll(
           GeoDocumentPart(Some(0), "Part 1", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord01Bordeaux.html", 0),
           GeoDocumentPart(Some(1), "Part 2", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord02Milan.html", 0),
           GeoDocumentPart(Some(2), "Part 3", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord03Sirmium.html", 0),
           GeoDocumentPart(Some(3), "Part 4", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord04Const.html", 0),
           GeoDocumentPart(Some(4), "Part 5", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord05Antiochia.html", 0),
           GeoDocumentPart(Some(5), "Part 6", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord06Caesarea.html", 0),
           GeoDocumentPart(Some(6), "Part 7a", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord07aJerus.html", 0),
           GeoDocumentPart(Some(7), "Part 7b", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord07bJerus.html", 0),
           GeoDocumentPart(Some(8), "Part 8a", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord08aJerusSurr.html", 0),
           GeoDocumentPart(Some(9), "Part 8b", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord08bJerusSurr.html", 0),
           GeoDocumentPart(Some(10), "Part 9", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord09Heraclea.html", 0),
           GeoDocumentPart(Some(11), "Part 10", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord10Valona.html", 0),
           GeoDocumentPart(Some(12), "Part 11", "http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord11Rome.html", 0))
      }
      
      if (MTable.getTables("annotations").list().isEmpty) {
        Annotations.ddl.create
        
        val csv = Seq(
          "public/test/part1.csv",
          "public/test/part2.csv",
          "public/test/part3.csv",
          "public/test/part4.csv",
          "public/test/part5.csv",
          "public/test/part6.csv",
          "public/test/part7a.csv",
          "public/test/part7b.csv",
          "public/test/part8a.csv",
          "public/test/part8b.csv",
          "public/test/part9.csv",
          "public/test/part10.csv",
          "public/test/part11.csv").zipWithIndex
          
        csv.foreach { case (file, gdocPartId) => {
          CSVImporter.importAnnotations(file, gdocPartId).foreach(annotation => Annotations.insert(annotation))
        }}
      }
    }
  }  

}