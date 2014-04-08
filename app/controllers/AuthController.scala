package controllers

import global.Global
import models.Users
import play.api.data.Form
import play.api.db.slick.{ DBAction, DBSessionRequest }
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.db.slick.DBSessionRequest
import scala.slick.session.Session
import models.User
import play.api.Logger

/** Authentication based on username & password.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
object AuthController extends Controller {

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
        val hash = User.computeHash(user.get.salt + password)
        user.get.hash.equals(hash)
      } else {
        false
      }
    }
  }

  /** Login page **/
  def login(destination: Option[String]) = Action { implicit request => Ok(views.html.login(loginForm, destination)) }

  /** Login POST handler **/
  def authenticate = Action { implicit request =>
    val destFormVal = request.body.asFormUrlEncoded.map(_.get("destination")).flatten
    val destination =
      if (destFormVal.isDefined && destFormVal.get.size > 0)
        Some(destFormVal.get.head)
      else
        None
        
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, destination)),
      user => {
        if (destination.isDefined) {
          Redirect(destination.get).withSession(Security.username -> user._1)
        } else {
          Redirect(routes.ApplicationController.index(None)).withSession(Security.username -> user._1)
        }
      }
    )
  }

  /** Logout handler **/
  def logout = Action {
    Redirect(routes.ApplicationController.index(None)).withNewSession
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
  
  def currentUser(implicit request: RequestHeader, session: Session) =
    username(request).map(username => Users.findByUsername(username)).flatten
  
  private def onUnauthorized(policy: Secure.Policy)(request: RequestHeader) = {
    if (policy == Secure.REDIRECT_TO_LOGIN)
      Results.Redirect(routes.AuthController.login(Some(request.uri)))
    else
      Results.Forbidden
  } 
  
  def isAuthorized(implicit request: RequestHeader) = username(request).isDefined
  
  /** For protected actions **/
  def protectedAction(policy: Secure.Policy)(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized(policy)) { username =>
      Action(request => f(username)(request))
    }
  }

  /** For protected actions that require DB access **/
  def protectedDBAction(policy: Secure.Policy)(f: => String => DBSessionRequest[AnyContent] => SimpleResult) = {
    Security.Authenticated(username, onUnauthorized(policy)) { username =>
      DBAction(BodyParsers.parse.anyContent)(rs => f(username)(rs))
    }
  }

  /** For Actions that require Admin status **/
  def adminAction(f: => String => DBSessionRequest[AnyContent] => SimpleResult) =
    protectedDBAction(Secure.REJECT) { username => rs =>
      val isAdmin = Users.findByUsername(username)(rs.dbSession).map(_.isAdmin).getOrElse(false)
      if (isAdmin)
        f(username)(rs)
      else 
        Results.Forbidden
    }
  
}