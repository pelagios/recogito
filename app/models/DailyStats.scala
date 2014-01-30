package models

import java.sql.Timestamp
import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

case class DailyStats(id: Option[Int], timestamp: Timestamp, verifiedToponyms: Int, unverifiedToponyms: Int, unidentifiableToponyms: Int, totalEdits: Int) {
  
  lazy val totalToponyms = verifiedToponyms + unverifiedToponyms + unidentifiableToponyms
  
}

/** Annotation database table **/
object StatsHistory extends Table[DailyStats]("stats_history") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def timestamp = column[Timestamp]("timestamp")
  
  def verifiedToponyms = column[Int]("verified_toponyms")
  
  def unverifiedToponyms = column[Int]("unverified_toponyms")
  
  def unidentifiableToponyms = column[Int]("unidentifiable_toponyms")
  
  def totalEdits = column[Int]("total_edits")
  
  def * = id.? ~ timestamp ~ verifiedToponyms ~ unverifiedToponyms ~ 
    unidentifiableToponyms ~ totalEdits <> (DailyStats.apply _, DailyStats.unapply _)
  
}
