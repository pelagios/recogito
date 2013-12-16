package org.pelagios.grct

import models._
import java.io.File
import org.openrdf.rio.RDFFormat
import org.pelagios.grct.index.PlaceIndex
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.DB
import play.api.Play.current
import scala.slick.session.Database
import play.api.db.slick.Config.driver.simple._
import scala.slick.jdbc.meta.MTable
import org.pelagios.grct.importer.CSVImporter
import scala.io.Source

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
      
        // Dummy users (temporary)
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
           GeoDocumentPart(Some(0), 0, "Part 1", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord01Bordeaux.html")),
           GeoDocumentPart(Some(1), 0, "Part 2", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord02Milan.html")),
           GeoDocumentPart(Some(2), 0, "Part 3", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord03Sirmium.html")),
           GeoDocumentPart(Some(3), 0, "Part 4", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord04Const.html")),
           GeoDocumentPart(Some(4), 0, "Part 5", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord05Antiochia.html")),              
           GeoDocumentPart(Some(5), 0, "Part 6", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord06Caesarea.html")),
           GeoDocumentPart(Some(6), 0, "Part 7a", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord07aJerus.html")),
           GeoDocumentPart(Some(7), 0, "Part 7b", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord07bJerus.html")),
           GeoDocumentPart(Some(8), 0, "Part 8a", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord08aJerusSurr.html")),
           GeoDocumentPart(Some(9), 0, "Part 8b", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord08bJerusSurr.html")),
           GeoDocumentPart(Some(10), 0, "Part 9", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord09Heraclea.html")),
           GeoDocumentPart(Some(11), 0, "Part 10", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord10Valona.html")),
           GeoDocumentPart(Some(12), 0, "Part 11", Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord11Rome.html")))
      }
      
      if (MTable.getTables("gdocument_texts").list().isEmpty) {
        GeoDocumentTexts.ddl.create
        
        GeoDocumentTexts.insertAll(
            GeoDocumentText(Some(0), 0, Some(0), Source.fromFile("public/test/part1.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(1), 0, Some(1), Source.fromFile("public/test/part2.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(2), 0, Some(2), Source.fromFile("public/test/part3.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(3), 0, Some(3), Source.fromFile("public/test/part4.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(4), 0, Some(4), Source.fromFile("public/test/part5.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(5), 0, Some(5), Source.fromFile("public/test/part6.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(6), 0, Some(6), Source.fromFile("public/test/part7a.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(7), 0, Some(7), Source.fromFile("public/test/part7b.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(8), 0, Some(8), Source.fromFile("public/test/part8a.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(9), 0, Some(9), Source.fromFile("public/test/part8b.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(10), 0, Some(10), Source.fromFile("public/test/part9.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(11), 0, Some(11), Source.fromFile("public/test/part10.txt").map(_.toByte).toArray),
            GeoDocumentText(Some(12), 0, Some(12), Source.fromFile("public/test/part11.txt").map(_.toByte).toArray))
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
          CSVImporter.importAnnotations(file, 0, gdocPartId).foreach(annotation => Annotations.insert(annotation))
        }}
      }
      
      if (MTable.getTables("edit_history").list().isEmpty) {
        EditHistory.ddl.create        
      }
    }
  }  

}