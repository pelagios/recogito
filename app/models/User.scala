package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import java.security.MessageDigest
import java.math.BigInteger
import sun.security.provider.SecureRandom
import org.apache.commons.codec.binary.Base64

/** User case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */ 
case class User(username: String, hash: String, salt: String, editableDocuments: String = "*", isAdmin: Boolean = false) {

  def canEdit(docId: Int): Boolean = {
    if (editableDocuments.trim.equals("*"))
      true
    else
      editableDocuments.split(",").map(_.toInt).contains(docId)
  }
  
}

object User {

  private val MD5 = "MD5"
    
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

/** User database table **/
object Users extends Table[User]("users") {
  
  def username = column[String]("username", O.PrimaryKey)
  
  def hash = column[String]("hash", O.NotNull)
  
  def salt = column[String]("salt", O.NotNull)
  
  def editableDocuments = column[String]("editable_documents", O.NotNull)
  
  def isAdmin = column[Boolean]("is_admin")
  
  def * = username ~ hash ~ salt ~ editableDocuments ~ isAdmin <> (User.apply _, User.unapply _)
  
  def listAll()(implicit s: Session): Seq[User] = Query(Users).list
  
  def findByUsername(username: String)(implicit s: Session): Option[User] =
    Query(Users).where(_.username === username).firstOption
    
  /** Update an annotation **/
  def update(user: User)(implicit s: Session) =
    Query(Users).where(_.username === user.username).update(user)
  
}