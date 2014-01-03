package controllers

import models.Users
import org.pelagios.recogito.Global
import play.api.data.Form
import play.api.db.slick.{ DBAction, DBSessionRequest }
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue
import scala.slick.session.Session
import play.api.db.slick.DBSessionRequest

/** Authentication based on username & password.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
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

  /** Checks username and password against the database 
    *
    * @param username the username
    * @param password the password
    */
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
      user => Redirect(routes.ApplicationController.index()).withSession(Security.username -> user._1)
    )
  }

  /** Logout handler **/
  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

}

/** Security 'policy' enum **/
object Secure extends Enumeration {
  
  type Policy = Value
  
  /** Redirect unauthorized users to the login page **/
  val REDIRECT_TO_LOGIN = Value("redirect")
  
  /** Reject unauthorized users with an HTTP Forbidden status **/
  val REJECT = Value("reject")
  
}

trait Secured {
  
  private def username(request: RequestHeader) = request.session.get(Security.username)
  
  private def onUnauthorized(policy: Secure.Policy)(request: RequestHeader) = {
    if (policy == Secure.REDIRECT_TO_LOGIN)
      Results.Redirect(routes.Auth.login)
    else
      Results.Forbidden
  }
  
  
  /** For protected actions **/
  def protectedAction(policy: Secure.Policy)(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized(policy)) { user =>
      Action(request => f(user)(request))
    }
  }

  /** For protected actions that require DB access **/
  def protectedDBAction(policy: Secure.Policy)(f: => String => DBSessionRequest[AnyContent] => SimpleResult) = {
    Security.Authenticated(username, onUnauthorized(policy)) { user =>
      DBAction(BodyParsers.parse.anyContent)(rs => f(user)(rs))
    }
  }
  
}