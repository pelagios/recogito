package controllers

import global.Global
import models.{ User, Users }
import play.api.data._
import play.api.data.Forms._
import play.api.db.DB
import play.api.db.slick.Config.driver.simple._
import play.api.mvc.{ Action, Controller, Security }
import play.api.Play.current
import scala.slick.session.Database

case class UserData(username: String, password: String, confirmPassword: String)

object UserController extends Controller {
  
  private val signupForm = Form(  
    mapping(
      "username" -> text,
      "password" -> text,
      "confirmPassword" -> text
    )(UserData.apply)(UserData.unapply)     
    
    verifying
      ("Passwords don't match", f => f.password == f.confirmPassword)
    
    verifying
      ("Username not available", f => { 
        Global.database.withSession { implicit s: Session =>
          val existing = Users.findByUsername(f.username)
          existing.isEmpty
        }}
      ) 
  )
    
  def signup = Action { implicit request =>
    Ok(views.html.signup(signupForm))
  }
  
  def processSignup = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.signup(formWithErrors)),
      userdata => Global.database.withSession { implicit s: Session =>
        val salt = User.randomSalt
        Users.insert(User(userdata.username, User.computeHash(salt + userdata.password), salt))
        Redirect(routes.ApplicationController.index()).withSession(Security.username -> userdata.username) 
      }
    )
  }

}
