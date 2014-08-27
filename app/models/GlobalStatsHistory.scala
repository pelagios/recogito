package models

import java.sql.Timestamp
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

/** Stats History Record case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class StatsHistoryRecord(id: Option[Int], timestamp: Timestamp, verifiedToponyms: Int, unidentifiableToponyms: Int, totalToponyms: Int, totalEdits: Int)

/** Global stats history database table **/
class GlobalStatsHistory(tag: Tag) extends Table[StatsHistoryRecord](tag, "global_stats_history") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def verifiedToponyms = column[Int]("verified_toponyms", O.NotNull)
  
  def unidentifiableToponyms = column[Int]("unidentifiable_toponyms", O.NotNull)
  
  def totalToponyms = column[Int]("total_toponyms", O.NotNull)
  
  def totalEdits = column[Int]("total_edits", O.NotNull)
    
  def * = (id.?, timestamp, verifiedToponyms, unidentifiableToponyms, totalToponyms, totalEdits) <> (StatsHistoryRecord.tupled, StatsHistoryRecord.unapply)
        
}
    
object GlobalStatsHistory {   
    
  private val query = TableQuery[GlobalStatsHistory]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(statsRecord: StatsHistoryRecord)(implicit s: Session) = query.insert(statsRecord)
  
  def insertAll(statsRecords: Seq[StatsHistoryRecord])(implicit s: Session) = query.insertAll(statsRecords:_*)
  
  def listAll()(implicit s: Session): Seq[StatsHistoryRecord] =
    query.sortBy(_.timestamp.asc).list
   
  def listRecent(limit: Int)(implicit s: Session): Seq[StatsHistoryRecord] =
    query.sortBy(_.timestamp.desc).take(limit).list.reverse
  
}
