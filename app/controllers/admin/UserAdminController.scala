package controllers.admin

import controllers.common.auth.{ Secure, Secured }
import java.sql.Timestamp
import models.{ User, Users }
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{ Controller, Security }
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.Logger

case class SignupData(username: String, password: String, confirmPassword: String)

/** Controller for the 'Users' section of the admin area.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object UserAdminController extends Controller with Secured {
  
  private val signupForm = Form(  
    mapping(
      "username" -> text,
      "password" -> text,
      "confirmPassword" -> text
    )(SignupData.apply)(SignupData.unapply)     
    
    verifying
      ("Passwords don't match", f => f.password == f.confirmPassword)
    
    verifying
      ("Username not available", f => { 
        DB.withSession { implicit s: Session =>
          val existing = Users.findByUsername(f.username)
          existing.isEmpty
        }}
      ) 
  )
  
  /** Index page listing all documents in the DB **/
  def listAll = adminAction { username => implicit session =>
    Ok(views.html.admin.users(Users.listAll()))
  }
  
  /** Delete a user from the database **/
  def deleteUser(user: String) = protectedDBAction(Secure.REJECT) { username => implicit session =>
    Logger.info("Deleting user '" + user + "' from database")
    Users.delete(user)
    Status(200)
  }
    
  def signup = adminAction { username => implicit session =>
    Ok(views.html.admin.signup(signupForm))
  }
  
  def processSignup = adminAction { username => implicit session =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.admin.signup(formWithErrors)),
      data => DB.withSession { s: Session =>
        val salt = Users.randomSalt
        Users.insert(User(data.username, Users.computeHash(salt + data.password), salt, new Timestamp(System.currentTimeMillis)))(s)
        Redirect(controllers.admin.routes.UserAdminController.listAll) 
      }
    )
  }

}