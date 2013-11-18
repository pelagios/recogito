package org.pelagios.georescorrector

import scala.collection.JavaConverters._
import scala.io.Source
import unfiltered.jetty.Http
import unfiltered.filter.Plan
import unfiltered.request.{ GET, Path, Seg }
import unfiltered.response.{ CssContent, HtmlContent, JsonContent, NotFound, Ok, Redirect, ResponseString, ResponseBytes }
import org.pelagios.georescorrector.connector.SpreadsheetConnector
import org.pelagios.georescorrector.model._
import org.pelagios.georescorrector.index.PlaceIndex
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import unfiltered.response.JsContent
import java.io.BufferedInputStream
import scala.collection.immutable.Stream

/** Embedded app server.
  *  
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
class AppServer extends Plan {
  
  Global.writePIDToFile("pid.txt")
  
  val logger = LoggerFactory.getLogger(this.getClass)
  
  // We'll just grab the handle so that it gets created on startup if it does not exist yet
  private val index = Global.index
  
  def intent = { 
    case GET(Path("/gdoc-viewer/static")) => {
      Redirect("/gdoc-viewer/static/")      
    }
    
    case GET(Path("/gdoc-viewer/static/")) => {
      HtmlContent ~> ResponseString(Source.fromFile("assets/index.html").getLines.mkString("\n")) 
    }
    
    case GET(Path(Seg("gdoc-viewer" :: "static" :: asset))) => {
      val path = asset.mkString("/")
      logger.info("Returning " + path)
      val is = new FileInputStream("assets/" + path)
      if (is != null && path.endsWith("html")) {
        HtmlContent ~> ResponseString(Source.fromInputStream(is).getLines.mkString("\n"))
      } else if (is != null && path.endsWith("css")) {
        CssContent ~> ResponseString(Source.fromInputStream(is).getLines.mkString("\n"))
      } else if (is != null && path.endsWith("png")) {
        val bis = new BufferedInputStream(is)
        val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
        Ok ~> ResponseBytes(bArray)
      } else if (is != null && (path.endsWith("js") || path.endsWith("json"))) {
        JsContent ~> ResponseString(Source.fromInputStream(is).getLines.mkString("\n"))
      } else {
        NotFound
      }
    }
    
    case GET(Path(Seg("gdoc-viewer" :: "search" :: query :: Nil))) => {  
      val results = Global.index.query(query).map(place => {
        var names = place.names.map(_.labels).flatten.map(_.label).mkString(", ")
        var coords = place.getCentroid.map(coords => ", \"coords\": [" + coords.y + "," + coords.x + "]")
        "    { \"title\": " + "\"" + place.title + "\", \"names\":\"" + names + "\"" + coords.getOrElse("") + " }"
      }).mkString(",\n")
      
      val jsonResponse =
        "{\n  \"query\": \"" + query + "\",\n" +
        "  \"results\": [\n" + results + "\n  ]\n" +
        "}"
      
      JsonContent ~> ResponseString(jsonResponse)      
    }
    
    case GET(Path(Seg("gdoc-viewer" :: "api" :: id :: Nil))) => {
      logger.info("Request for spreadsheet " + id)
      val connector = new SpreadsheetConnector()
      val spreadsheet = connector.getSpreadsheet(id)
      
      val responseJSON = 
        if (spreadsheet.isDefined) {
          val worksheets = spreadsheet.get.getWorksheets().asScala
          logger.info("Found " + worksheets.size + " worksheets for " + id)
          
          val indexSheet = worksheets.find(w => w.getTitle.getPlainText.toLowerCase.contains("index"))
          val index = indexSheet.map(sheet => new IndexModel(connector.getCSV(sheet)))
          
          val datasheets = worksheets.filter(w => !w.getTitle.getPlainText.toLowerCase.contains("index"))
          
          val csv = datasheets.map(sheet => {
            val sourceURI = index.map(_.getSourceURI(sheet.getTitle.getPlainText)).flatten
            connector.getCSV(sheet, Some(sheet.getTitle.getPlainText), sourceURI)
          })
          logger.info("Got CSV data for " + id)
          new MapModel(csv).toJSON
        } else {
          logger.info(id + " not found")
          "{ \"error:\": \"Bad luck.\" }"
        }      
      
      logger.info("Constructed JSON repsonse for " + id)
      JsonContent ~> ResponseString(responseJSON)
    }
  }
  
}

/** Embedded app server **/
object AppServer extends App {
  
  val logger = LoggerFactory.getLogger(this.getClass)
  
  val hostname = if (args.length > 1) args(1) else "localhost"   
  logger.info("Binding to " + hostname)
  
  val port = if (args.length > 2) args(2).toInt else 8090
  logger.info("Starting on port " + port)
  
  Http.local(port).filter(new AppServer).run  

}