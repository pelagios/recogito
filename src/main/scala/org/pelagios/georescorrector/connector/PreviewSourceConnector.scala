package org.pelagios.georescorrector.connector

import scala.io.Source
import org.jsoup.Jsoup

class PreviewSourceConnector(url: String) {
  
  private val BUFFER = 40
  
  private val text = Jsoup.connect(url).get().text()
  
  private def extractSnippets(text: String, term: String, previous: Seq[String] = Seq.empty[String]): Seq[String] = {
    val startIdx = text.indexOf(term)
    if (startIdx > -1) {
      val endIdx = startIdx + term.size
      
      val snippetStart = if (startIdx - BUFFER > -1) startIdx - BUFFER else 0
      val snippetEnd = if (endIdx + BUFFER <= text.size) endIdx + BUFFER else text.size
      
      val snippet = text.substring(snippetStart, snippetEnd)
      extractSnippets(text.substring(endIdx), term, snippet +: previous)
    } else {
      previous 
    }
  }
  
  def getSnippets(term: String): Seq[String] = extractSnippets(text, term)
  
}