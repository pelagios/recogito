package controllers

import org.jsoup.Jsoup
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json

object Preview extends Controller {
  
  private val CHAR_BUFFER = 40

  def index(url: String, term: String) = Action {
    val text = Jsoup.connect(url).get().text()
    val snippets = extractSnippets(text, term)
    Ok(Json.toJson(snippets))
  }
  
  private def extractSnippets(text: String, term: String, previous: Seq[String] = Seq.empty[String]): Seq[String] = {
    val startIdx = text.indexOf(term)
    if (startIdx > -1) {
      val endIdx = startIdx + term.size
      
      val snippetStart = if (startIdx - CHAR_BUFFER > -1) startIdx - CHAR_BUFFER else 0
      val snippetEnd = if (endIdx + CHAR_BUFFER <= text.size) endIdx + CHAR_BUFFER else text.size
      
      val snippet = text.substring(snippetStart, snippetEnd)
      extractSnippets(text.substring(endIdx), term, snippet +: previous)
    } else {
      previous 
    }
  }
  
}