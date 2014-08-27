package global

import java.sql.Timestamp
import java.util.{ Calendar, Date }
import java.util.concurrent.TimeUnit
import models._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration
import scala.concurrent.duration.Duration
import play.api.Logger
import play.api.Play
import models.GlobalStatsHistory

object StatsDemon {
  
  // Log interval in hours (defaults to 24)
  private val interval =
    Duration(Play.current.configuration.getInt("recogito.stats.interval").getOrElse(24), TimeUnit.HOURS)
  
  // Log start time (defaults to 2am)
  private val delay = {
    val startTime = Play.current.configuration.getString("recogito.stats.starttime").getOrElse("2:00").split(":")
    if (startTime.size != 2)
      throw new IllegalArgumentException("Configuration contains invalid stats logger start time setting")
    
    // Create a calendar with today's date & start time from app.conf
    val now = new Date();
    val c = Calendar.getInstance()
    c.setTime(now)
    c.set(Calendar.HOUR_OF_DAY, startTime(0).toInt)
    c.set(Calendar.MINUTE, startTime(1).toInt)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    
    // Are we already past that time today? Set tomorrow.
    if (c.getTimeInMillis < now.getTime)
      c.add(Calendar.DAY_OF_MONTH, 1)
      
    // Time until start time
    Duration(c.getTimeInMillis - now.getTime, TimeUnit.MILLISECONDS)
  }

  def start() = {    
    Akka.system.scheduler.schedule(delay, interval) {
      
      DB.withSession { implicit s: Session =>
        Logger.debug("Logging stats...")
        val stats = Annotations.getCompletionStats().values.foldLeft(0, 0, 0) { case((verified, yellow, total), stats)  =>
          (verified + stats.verified, yellow + stats.yellow, total + stats.total) }
        
        val lastLogTime = new Date().getTime - interval.toMillis        
        val editsSinceLastLog = EditHistory.countSince(new Timestamp(lastLogTime))

        val statsHistoryRecord = StatsHistoryRecord(None, new Timestamp(new Date().getTime), stats._1, stats._2, stats._3, editsSinceLastLog)
        GlobalStatsHistory.insert(statsHistoryRecord)
            
        Logger.info("Stats: V-" + statsHistoryRecord.verifiedToponyms + ", UI-" + statsHistoryRecord.unidentifiableToponyms + 
          ", TOTAL:" + statsHistoryRecord.totalEdits + ", EDITS:" + statsHistoryRecord.totalEdits)
      }
    }
  }

}