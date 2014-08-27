package models

import java.sql.Timestamp
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class CollectionStatsHistoryRecord(id: Option[Int], collection: String, timestamp: Timestamp, verifiedToponyms: Int, unidentifiableToponyms: Int, totalToponyms: Int, totalEdits: Int)

class CollectionStatsHistory(tag: Tag) extends Table[CollectionStatsHistoryRecord](tag, "collection_stats_history") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def collection = column[String]("collection", O.NotNull)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def verifiedToponyms = column[Int]("verified_toponyms", O.NotNull)
  
  def unidentifiableToponyms = column[Int]("unidentifiable_toponyms", O.NotNull)
  
  def totalToponyms = column[Int]("total_toponyms", O.NotNull)
  
  def totalEdits = column[Int]("total_edits", O.NotNull)
    
  def * = (id.?, collection, timestamp, verifiedToponyms, unidentifiableToponyms, totalToponyms, totalEdits) <> (CollectionStatsHistoryRecord.tupled, CollectionStatsHistoryRecord.unapply)
  
}

object CollectionStatsHistory {   
    
  private val query = TableQuery[CollectionStatsHistory]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(statsRecord: CollectionStatsHistoryRecord)(implicit s: Session) = query.insert(statsRecord)
  
  def insertAll(statsRecords: Seq[CollectionStatsHistoryRecord])(implicit s: Session) = query.insertAll(statsRecords:_*)
  
  def listAll()(implicit s: Session): Seq[CollectionStatsHistoryRecord] =
    query.sortBy(_.timestamp.asc).list
   
  def listRecent(limit: Int)(implicit s: Session): Seq[CollectionStatsHistoryRecord] =
    query.sortBy(_.timestamp.desc).take(limit).list.reverse
  
}