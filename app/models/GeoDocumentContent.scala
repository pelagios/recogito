package models

/** Generic 'interface' for different content types **/
trait GeoDocumentContent {
	
 def id: Option[Int]
 
 def gdocId: Int
 
 def gdocPartId: Option[Int]	
 
}

