package models

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class SignOff(id: Option[Int], gdocId: Int, gdocPartId: Option[Int], username: String, timestamp: Timestamp)

class SignOffs(tag: Tag) extends Table[SignOff](tag, "signoffs") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.NotNull)
  
  def gdocPartId = column[Int]("gdoc_part", O.Nullable)
  
  def username = column[String]("username", O.NotNull)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def * = (id.?, gdocId, gdocPartId.?, username, timestamp) <> (SignOff.tupled, SignOff.unapply)
  
  /** Foreign key constraints **/
  
  def gdocFk = foreignKey("gdoc_fk", gdocId, TableQuery[GeoDocuments])(_.id)
  
  def gdocPartFk = foreignKey("gdoc_part_fk", gdocPartId, TableQuery[GeoDocumentParts])(_.id)
  
}

object SignOffs {
  
  private val query = TableQuery[SignOffs]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def countForGeoDocument(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocId === id).length).first
  
  def countForGeoDocumentPart(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocPartId === id).length).first
  
}

