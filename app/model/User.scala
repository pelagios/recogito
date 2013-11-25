package model

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

case class User(id: Option[Long] = None, username: String, password: String)

object Users extends Table[User]("user") {
  
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  
  def username = column[String]("username")
  
  def password = column[String]("password")
  
  def * = id.? ~ username ~ password <> (User.apply _, User.unapply _)
  
  def findByUsername(username: String)(implicit s: Session): Option[User] = {
    Query(Users).where(_.username === username).firstOption
  }
  
}