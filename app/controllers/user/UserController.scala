package controllers.user

import global.Global
import models.{ User, Users, EditHistory }
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{ Session => PlaySession, _ }
import play.api.db.slick._
import play.api.Play.current
import controllers.common.auth.{ Secure, Secured }

case class ChangePasswordData(oldPassword: String, newPassword: String, confirmPassword: String)

object UserController extends Controller with Secured {
  
  private val changePasswordForm = Form(  
    mapping(
      "oldPassword" -> text,
      "newPassword" -> text,
      "confirmPassword" -> text
    )(ChangePasswordData.apply)(ChangePasswordData.unapply)     
    
    verifying
      ("Passwords don't match", f => f.newPassword == f.confirmPassword)
  )
  
  def showPublicProfile(username: String) = DBAction { implicit request =>
    val user = Users.findByUsername(username)
    if (user.isDefined) {
      val numberOfEdits = EditHistory.countForUser(username)
      val numberOfEditsPerDocument = EditHistory.countForUserPerDocument(username)
      Ok(views.html.stats.userStats(user.get, numberOfEdits, numberOfEditsPerDocument))
    } else {
      NotFound
    } 
  }
  
  def showMySettings = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit session =>
    Ok(views.html.changePassword(changePasswordForm))
  }
  
  def changePassword = protectedDBAction(Secure.REDIRECT_TO_LOGIN) { username => implicit session => 
    changePasswordForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.changePassword(formWithErrors)),
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
          val newSalt = Users.randomSalt
          val newHash = Users.computeHash(newSalt + data.newPassword)
          Users.update(User(username, newHash, newSalt, user.get.memberSince, user.get.editableDocuments, user.get.isAdmin))(s)
          Redirect(routes.UserController.changePassword).flashing("success" -> "Your password was successfully changed")
        } else {
          Redirect(routes.UserController.changePassword).flashing("error" -> "Invalid current password")
        }
      }
    )
  }

}
