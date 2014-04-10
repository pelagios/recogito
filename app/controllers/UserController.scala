package controllers

import global.Global
import models.{ User, Users }
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{ Session => PlaySession, _ }
import play.api.db.slick._
import play.api.Play.current

case class SignupData(username: String, password: String, confirmPassword: String)

case class ChangePasswordData(oldPassword: String, newPassword: String, confirmPassword: String)

object UserController extends Controller with Secured {
  
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
  
  private val changePasswordForm = Form(  
    mapping(
      "oldPassword" -> text,
      "newPassword" -> text,
      "confirmPassword" -> text
    )(ChangePasswordData.apply)(ChangePasswordData.unapply)     
    
    verifying
      ("Passwords don't match", f => f.newPassword == f.confirmPassword)
  )
    
  def signup = adminAction { username => implicit session =>
    Ok(views.html.signup(signupForm))
  }
  
  def processSignup = adminAction { username => implicit session =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.signup(formWithErrors)),
      data => DB.withSession { s: Session =>
        val salt = Users.randomSalt
        Users.insert(User(data.username, Users.computeHash(salt + data.password), salt))(s)
        Redirect(routes.ApplicationController.index(None)).withSession(Security.username -> data.username) 
      }
    )
  }
  
  def changePassword = adminAction { username => implicit session =>
    Ok(views.html.user_settings(changePasswordForm))
  }
  
  def processChangePassword = adminAction { username => implicit session => 
    changePasswordForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.user_settings(formWithErrors)),
      data => DB.withSession { s: Session =>
        val user = Users.findByUsername(username)(s)
        val valid = 
          if (user.isDefined) {
            val hash = Users.computeHash(user.get.salt + data.oldPassword)
            user.get.hash.equals(hash)
          } else {
            false
          }
      
        if (valid) {
          val newHash = Users.computeHash(user.get.salt + data.newPassword)
          Users.update(User(username, newHash, user.get.salt, user.get.editableDocuments, user.get.isAdmin))(s)
          Redirect(routes.UserController.changePassword).flashing("success" -> "Your password was successfully changed")
        } else {
          Redirect(routes.UserController.changePassword).flashing("error" -> "Invalid current password")
        }
      }
    )
  }

}
