package controllers

import com.sun.syndication.io.WireFeedOutput
import com.sun.syndication.feed.atom.{ Content, Entry, Feed, Person }
import java.io.StringWriter
import java.util.Date
import models.{ AnnotationStatus, EditHistory }
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.Play
import play.api.Play.current
import scala.collection.JavaConversions._

object FeedController extends Controller {
  
  private val NUMBER_OF_ENTRIES = Play.current.configuration.getInt("recogito.feed.entries").getOrElse(20)
  
  private val ATOM_1_0 = "atom_1.0"
  
  def recentVerifications = DBAction { implicit session =>
    val mostRecent = EditHistory.getLastN(NUMBER_OF_ENTRIES, AnnotationStatus.VERIFIED)    
    val baseURL = routes.FeedController.recentVerifications.absoluteURL(false)
    
    val feed = new Feed()
    feed.setFeedType(ATOM_1_0)
    feed.setTitle("Recogito - Recent Verifications")
    feed.setId(baseURL)
    feed.setUpdated(new Date(mostRecent.head.timestamp.getTime))

    val entries = mostRecent.map(editEvent => {
      val entry = new Entry()
      entry.setTitle(editEvent.annotationId.toString)
      entry.setUpdated(new Date(editEvent.timestamp.getTime))
      entry.setId(baseURL + "#" + editEvent.annotationId.toString)
      
      val author = new Person()
      author.setName(editEvent.username)
      entry.setAuthors(Seq(author))
            
      val content = new Content()
      content.setType("text/plain")
      content.setValue(editEvent.annotationAfter.toString)
      entry.setContents(Seq(content))

      entry
    })
    
    feed.setEntries(entries)
    
    val wirefeed = new WireFeedOutput()
    val writer = new StringWriter()
    wirefeed.output(feed, writer)
    writer.flush()
    writer.close()
    Ok(writer.toString)
  }

}