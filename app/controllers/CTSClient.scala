package controllers

import scala.io.Source
import scala.xml.XML
import java.net.URL

/** A trait that adds CTS client functionality **/
trait CTSClient {
  
  def getPlaintext(uri: String) = {
    val xml = XML.load(new URL(uri))
    val paragraphs = xml \\ "p"
    paragraphs.map(_.text.trim + "\n").mkString("\n")
  }

}