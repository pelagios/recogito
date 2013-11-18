package org.pelagios.georescorrector.model

/** A simple wrapper to represent CSV-style data.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
class CSVModel(val colHeadings: Seq[String], val rows: Seq[Seq[String]], val title:Option[String] = None, val sourceURI: Option[String] = None)