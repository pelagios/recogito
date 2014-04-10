package models

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class StatsRecord(id: Option[Int], timestamp: Timestamp, verifiedToponyms: Int, unverifiedToponyms: Int, unidentifiableToponyms: Int, totalEdits: Int) {
  
  lazy val totalToponyms = verifiedToponyms + unverifiedToponyms + unidentifiableToponyms
  
}

/** Annotation database table **/
class StatsHistory(tag: Tag) extends Table[StatsRecord](tag, "stats_history") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def timestamp = column[Timestamp]("timestamp")
  
  def verifiedToponyms = column[Int]("verified_toponyms")
  
  def unverifiedToponyms = column[Int]("unverified_toponyms")
  
  def unidentifiableToponyms = column[Int]("unidentifiable_toponyms")
  
  def totalEdits = column[Int]("total_edits")
  
  def * = (id.?, timestamp, verifiedToponyms, unverifiedToponyms, 
    unidentifiableToponyms, totalEdits) <> (StatsRecord.tupled, StatsRecord.unapply)
    
}
    
object StatsHistory {   
    
  private val query = TableQuery[StatsHistory]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(statsRecord: StatsRecord)(implicit s: Session) = query.insert(statsRecord)
  
  def insertAll(statsRecords: Seq[StatsRecord])(implicit s: Session) = query.insertAll(statsRecords:_*)
  
  def listAll()(implicit s: Session): Seq[StatsRecord] =
    query.list
  
}
