package controllers

import models.Users
import org.pelagios.grct.Global
import play.api.data.Form
import play.api.db.slick.{ DBAction, DBSessionRequest }
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue
import scala.slick.session.Session
import play.api.db.slick.DBSessionRequest

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
      user => Redirect(routes.Application.index(None)).withSession(Security.username -> user._1)
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

  /** For authenticated actions **/
  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }
  
  /** For authenticated actions with DB access **/
  def dbSessionWithAuth(f: => String => DBSessionRequest[_] => SimpleResult) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      DBAction(rs => f(user)(rs))
    }
  }
  
  /** For authenticated JSON actions with DB access **/
  def jsonWithAuth(f: => String => DBSessionRequest[JsValue] => SimpleResult) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      DBAction(BodyParsers.parse.json)(rs => f(user)(rs))
    }
  }
  
}