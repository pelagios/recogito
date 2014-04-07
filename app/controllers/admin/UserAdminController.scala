package controllers.admin

import controllers.{ Secure, Secured }
import models.Users
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Logger

/** Controller for the 'Users' section of the admin area.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object UserAdminController extends Controller with Secured {
  
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

}