package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** User case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */ 
case class User(username: String, password: String, editableDocuments: String = "*", isAdmin: Boolean = false) {

  def canEdit(docId: Int): Boolean = {
    if (editableDocuments.trim.equals("*"))
      true
    else
      editableDocuments.split(",").map(_.toInt).contains(docId)
  }
  
}

/** User database table **/
object Users extends Table[User]("users") {
  
  def username = column[String]("username", O.PrimaryKey)
  
  def password = column[String]("password")
  
  def editableDocuments = column[String]("editable_documents")
  
  def isAdmin = column[Boolean]("is_admin")
  
  def * = username ~ password ~ editableDocuments ~ isAdmin <> (User.apply _, User.unapply _)
  
  def listAll()(implicit s: Session): Seq[User] = Query(Users).list
  
  def findByUsername(username: String)(implicit s: Session): Option[User] =
    Query(Users).where(_.username === username).firstOption
  
}