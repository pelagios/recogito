package controllers

import model.Users
import org.pelagios.grct.Global
import play.api.data.Form
import play.api.mvc._
import play.api.db.slick.Config.driver.simple.Session
import play.api.Play.current

/** Authentication based on username & password **/
object Auth extends Controller {

  import play.api.data.Forms._
  
  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying ("Invalid username or password", result => result match {
      case (username, password) => check(username, password)
    })
  )

  /** Checks username and password against the database **/
  def check(username: String, password: String) = {
    Global.database.withSession { implicit s: Session =>
      val user = Users.findByUsername(username)
      if (user.isDefined) {
        user.get.password.equals(password)
      } else {
        false
      }
    }
  }

  /** Login page **/
  def login = Action { implicit request => Ok(views.html.login(loginForm)) }

  /** Login POST handler **/
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  /** Logout handler **/
  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

}

trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  /*
   * This method shows how you could wrap the withAuth method to also fetch your user
   * You will need to implement UserDAO.findOneByUsername
   *
  def withUser(f: User => Request[AnyContent] => Result) = withAuth { username => implicit request =>
    UserDAO.findOneByUsername(username).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }
  */

}