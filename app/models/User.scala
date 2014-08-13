package models

import java.math.BigInteger
import java.sql.Timestamp
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import sun.security.provider.SecureRandom

/** User case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */ 
case class User(username: String, hash: String, salt: String, memberSince: Timestamp, editableDocuments: String = "*", isAdmin: Boolean = false) {

  def canEdit(docId: Int): Boolean = {
    if (editableDocuments.trim.equals("*"))
      true
    else
      editableDocuments.split(",").map(_.toInt).contains(docId)
  }
  
}

/** User database table **/
class Users(tag: Tag) extends Table[User](tag, "users") {
  
  def username = column[String]("username", O.PrimaryKey)
  
  def hash = column[String]("hash", O.NotNull)
  
  def salt = column[String]("salt", O.NotNull)
  
  def memberSince = column[Timestamp]("member_since", O.NotNull)
  
  def editableDocuments = column[String]("editable_documents", O.NotNull)
  
  def isAdmin = column[Boolean]("is_admin")
  
  def * = (username, hash, salt, memberSince, editableDocuments, isAdmin) <> (User.tupled, User.unapply)
  
}

object Users {

  private val MD5 = "MD5"
      
  private[models] val query = TableQuery[Users]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(user: User)(implicit s: Session) = query.insert(user)
  
  def insertAll(users: Seq[User])(implicit s: Session) = query.insertAll(users:_*)
  
  def listAll()(implicit s: Session): Seq[User] = query.list
  
  def findByUsername(username: String)(implicit s: Session): Option[User] =
    query.where(_.username === username).firstOption
    
  def update(user: User)(implicit s: Session) =
    query.where(_.username === user.username).update(user)
      
  def delete(username: String)(implicit s: Session) =
    query.where(_.username === username).delete
    
  def randomSalt = {
    val r = new SecureRandom()
    val salt = new Array[Byte](32)
    r.engineNextBytes(salt)
    Base64.encodeBase64String(salt)
  }
  
  def computeHash(str: String) = {
    val md = MessageDigest.getInstance(MD5).digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }
  
}
