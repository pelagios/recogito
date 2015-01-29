package controllers.unrestricted

import play.api.mvc.{ Action, Controller }

object InfoPagesController extends Controller {
  
  def showAbout() = Action {
    Ok(views.html.about())
  }
  
  def showDocumentation() = Action {
    Redirect("/recogito/static/documentation/index.html")
  } 

}
