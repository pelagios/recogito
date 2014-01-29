package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** User case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */ 
case class User(id: Option[Int], username: String, password: String, editableDocuments: String = "*", isAdmin: Boolean = false)

/** User database table **/
object Users extends Table[User]("users") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def username = column[String]("username")
  
  def password = column[String]("password")
  
  def editableDocuments = column[String]("editable_documents")
  
  def isAdmin = column[Boolean]("is_admin")
  
  def * = id.? ~ username ~ password ~ editableDocuments ~ isAdmin <> (User.apply _, User.unapply _)
  
  def listAll()(implicit s: Session): Seq[User] = Query(Users).list
    
  def findById(id: Int)(implicit s: Session): Option[User] =
    Query(Users).where(_.id === id).firstOption
  
  def findByUsername(username: String)(implicit s: Session): Option[User] =
    Query(Users).where(_.username === username).firstOption
  
}