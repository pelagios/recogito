package global

import java.sql.Timestamp
import java.util.{ Calendar, Date }
import java.util.concurrent.TimeUnit
import models._
import play.api.db.DB
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration
import scala.slick.session.Database
import scala.concurrent.duration.Duration
import play.api.Logger

object StatsDemon {
  
  private val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000 

  def start(): Unit = {
    val delay = Duration(5000, TimeUnit.MILLISECONDS)
    val interval = Duration(1, TimeUnit.DAYS)
    
    Akka.system.scheduler.schedule(delay, interval) {
      import Database.threadLocalSession
      Database.forDataSource(DB.getDataSource()).withSession {
        Logger.info("Logging daily stats to database...")
        val docStats = GeoDocuments.listAll().map(doc => {
          val id = doc.id.get
          val verified = Annotations.countForGeoDocumentAndStatus(id, AnnotationStatus.VERIFIED)
          val unverified = Annotations.countForGeoDocumentAndStatus(id, AnnotationStatus.NOT_VERIFIED)
          val unidentifiable = Annotations.countForGeoDocumentAndStatus(id, AnnotationStatus.NOT_IDENTIFYABLE)
          (verified, unverified, unidentifiable)
        })

        val oneDayBeforeNow = new Date().getTime() - ONE_DAY_IN_MILLIS
        val totalEdits = EditHistory.countSince(new Timestamp(oneDayBeforeNow))
        val stats = docStats.foldLeft((0, 0, 0))((result, docStats) =>
          (result._1 + docStats._1, result._2 + docStats._2, result._3 + docStats._3))
          
        val dailyStats = DailyStats(None, new Timestamp(new Date().getTime), stats._1, stats._2, stats._3, totalEdits)
        StatsHistory.insert(dailyStats)
            
        Logger.info("Done - verified:" + dailyStats.verifiedToponyms + ", unverified:" + dailyStats.unverifiedToponyms + 
          ", unidentifiable:" + dailyStats.unidentifiableToponyms + ", edits:" + dailyStats.totalEdits)
      }
    }
  }
  
  
  private def timeTillMidnight = {
    val now = new Date()

    // Next day midnight
    val calendar = Calendar.getInstance()    
    calendar.setTime(now)
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    calendar.getTimeInMillis - now.getTime
  }

}