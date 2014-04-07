package controllers.admin

import controllers.Secured
import models.Users
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Controller for the 'Users' section of the admin area.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object UserAdminController extends Controller with Secured {
  
  /** Index page listing all documents in the DB **/
  def listAll = adminAction { username => implicit session =>
    Ok(views.html.admin.users(Users.listAll()))
  }

}