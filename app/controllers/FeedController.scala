package controllers

import play.api.mvc.Controller
import play.api.db.slick.DBAction
import play.api.Play.current
import models.EditHistory
import play.api.db.slick._
import models.AnnotationStatus
import com.sun.syndication.feed.synd.SyndFeedImpl
import com.sun.syndication.feed.synd.SyndEntryImpl
import com.sun.syndication.feed.synd.SyndContentImpl
import scala.collection.JavaConversions._
import com.sun.syndication.io.WireFeedOutput
import java.io.StringWriter
import java.io.Writer
import com.sun.syndication.feed.atom.{ Entry, Feed }
import java.util.Date
import com.sun.syndication.feed.atom.Person
import com.sun.syndication.feed.rss.Description
import com.sun.syndication.feed.atom.Content

object FeedController extends Controller {
  
  def recentVerifications = DBAction { implicit session =>
    // TODO make configurable
    val mostRecent = EditHistory.getLastN(20, AnnotationStatus.VERIFIED)
    
    val baseURL = routes.FeedController.recentVerifications.absoluteURL(false)
    
    val feed = new Feed()
    feed.setFeedType("atom_1.0")
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
    val writer: Writer = new StringWriter()
    wirefeed.output(feed, writer)
    writer.flush()
    writer.close()
    Ok(writer.toString)
  }

}