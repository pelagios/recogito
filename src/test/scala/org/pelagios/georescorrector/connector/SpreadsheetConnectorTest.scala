package org.pelagios.georescorrector.connector

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SpreadsheetConnectorTest extends FunSuite {

  /** Tests the connector against the live API.
    *  
    * Because we are testing against the live API, there is not
    * much stable data we can test against. Therefore, we just
    * log output and test whether the test completes without
    * crashing.
    */ 
  test("Test SpreadsheetConnector") {
    val connector = new SpreadsheetConnector()
    val spreadsheets = connector.listSpreadsheets()
    
    spreadsheets.foreach(spreadsheet => {
      val csv = connector.getCSV(spreadsheet.getWorksheets().get(7))
      println(csv.colHeadings)
      csv.rows.foreach(row => println(row))
    })    
  }
  
}