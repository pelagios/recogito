package org.pelagios.georescorrector.connector

import java.net.URL
import scala.collection.JavaConverters._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.client.spreadsheet.SpreadsheetService
import org.pelagios.georescorrector.model.CSVModel
import org.pelagios.georescorrector.Global

class SpreadsheetConnector {
  
  private val USERNAME = Global.username
  private val PASSWORD = Global.password
  
  private val service = new SpreadsheetService("MySpreadsheetIntegration-v1")
  service.setUserCredentials(USERNAME, PASSWORD)
  
  private val SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full")
  
  private val feed = service.getFeed(SPREADSHEET_FEED_URL, classOf[SpreadsheetFeed])
  
  def listSpreadsheets(): Seq[SpreadsheetEntry] = feed.getEntries().asScala
  
  def getSpreadsheet(key: String): Option[SpreadsheetEntry] = {
    val spreadsheets = listSpreadsheets().filter(_.getSpreadsheetLink().getHref().endsWith(key))    
    if (spreadsheets.size == 0)
      None
    else
      Some(spreadsheets(0))
  }
  
  /** Retrieves the header names from a worksheet.
    *  
    * The Google Spreadsheet API is incredibly painful in terms of handling spreadsheet
    * header names. The list feed will simply strip them off. The cell feed, on the
    * other hand, keeps the names intact, but is tedious to work with. This method
    * retrieves the cell feed and extracts only the header names.
    */
  private def getHeaderNames(worksheet: WorksheetEntry): Seq[String] = {
    val cellFeed = service.getFeed(worksheet.getCellFeedUrl, classOf[CellFeed])
    val cells = cellFeed.getEntries.asScala
    
    // Just the first row, just the cell values
    cells.takeWhile(_.getCell.getRow == 1).map(_.getCell.getValue)
  }
  
  def getCSV(worksheet: WorksheetEntry, title: Option[String] = None, sourceURI: Option[String] = None): CSVModel = {
    val headerNames = getHeaderNames(worksheet)
    
    val listFeed = service.getFeed(worksheet.getListFeedUrl, classOf[ListFeed])
    val rows = listFeed.getEntries.asScala
    
    val data = rows.map(row => {
      val tags = row.getCustomElements().getTags().asScala.toSeq
      tags.map(tag => row.getCustomElements.getValue(tag))
    })
    
    new CSVModel(headerNames, data, title, sourceURI)
  }

}