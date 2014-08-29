package models

import java.sql.Timestamp
import models.content._
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class SignOff(id: Option[Int], gdocTextId: Option[Int], gdocImageId: Option[Int], username: String, timestamp: Timestamp)

class SignOffs(tag: Tag) extends Table[SignOff](tag, "signoffs") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocTextId = column[Int]("gdoc_text", O.Nullable)
  
  def gdocImageId = column[Int]("gdoc_image", O.Nullable)
  
  def username = column[String]("username", O.NotNull)
  
  def timestamp = column[Timestamp]("timestamp", O.NotNull)
  
  def * = (id.?, gdocTextId.?, gdocImageId.?, username, timestamp) <> (SignOff.tupled, SignOff.unapply)
  
  /** Foreign key constraints **/
  
  def gdocTextFk = foreignKey("gdoc_text_fk", gdocTextId, TableQuery[GeoDocumentTexts])(_.id)
  
  def gdocImageFk = foreignKey("gdoc_image_fk", gdocImageId, TableQuery[GeoDocumentImages])(_.id)
  
  def userFk = foreignKey("user_fk", username, TableQuery[Users])(_.username)
  
}

object SignOffs {
  
  private val query = TableQuery[SignOffs]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def deleteForGeoDocumentText(gdocId: Int)(implicit s: Session) =
    query.where(_.gdocTextId === gdocId).delete
  
  def countForGeoDocumentText(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocTextId === id).length).first
    
  def countForGeoDocumentImage(id: Int)(implicit s: Session): Int =
    Query(query.where(_.gdocImageId === id).length).first
    
  def findForGeoDocumentText(id: Int)(implicit s: Session): Seq[(String, Timestamp)] =
    query.where(_.gdocTextId === id).map(row => (row.username, row.timestamp)).list
    
  def findForGeoDocumentImage(id: Int)(implicit s: Session): Seq[(String, Timestamp)] =
    query.where(_.gdocImageId === id).map(row => (row.username, row.timestamp)).list
    
  def toggleStatusForText(id: Int, username: String)(implicit s: Session) = {
    val q = query.where(_.gdocTextId === id).filter(_.username === username)
    q.firstOption match {
      case Some(signoff) => {
        q.delete
        false
      }
      
      case None => {
        query.insert(SignOff(None, Some(id), None, username, new Timestamp(System.currentTimeMillis)))
        true
      }
    }
  }
    
  def toggleStatusForImage(id: Int, username: String)(implicit s: Session) = {
    val q = query.where(_.gdocImageId === id).filter(_.username === username)
    q.firstOption match {
      case Some(signoff) => {
        q.delete
        false
      }
      
      case None => {
        query.insert(SignOff(None, None, Some(id), username, new Timestamp(System.currentTimeMillis)))
        true
      }
    }
  }
  
}

