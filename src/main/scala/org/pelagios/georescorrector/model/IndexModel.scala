package org.pelagios.georescorrector.model

class IndexModel(csv: CSVModel) {
  
  private val IDX_WORKSHEET = csv.colHeadings.indexWhere(_.equalsIgnoreCase("worksheet"))
  private val IDX_SOURCE = csv.colHeadings.indexWhere(_.equalsIgnoreCase("source"))
  
  def getSourceURI(worksheetTitle: String): Option[String] = {
    val rows = csv.rows.filter(row => row(IDX_WORKSHEET).equalsIgnoreCase(worksheetTitle))
    if (rows.size > 0) {
      val firstRow = rows(0)
      Some(firstRow(IDX_SOURCE))    
    } else {
      None
    }
  }

}