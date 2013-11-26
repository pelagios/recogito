package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** User case class **/ 
case class User(id: Option[Int] = None, username: String, password: String)

/** User database table **/
object Users extends Table[User]("user") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def username = column[String]("username")
  
  def password = column[String]("password")
  
  def * = id.? ~ username ~ password <> (User.apply _, User.unapply _)
  
  def findByUsername(username: String)(implicit s: Session): Option[User] = {
    Query(Users).where(_.username === username).firstOption
  }
  
}