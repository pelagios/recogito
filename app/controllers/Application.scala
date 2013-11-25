package controllers

import play.api.mvc.{ Action, Controller }

object Application extends Controller with Secured {
  
  def index() = withAuth { username => implicit request =>
    Ok(views.html.index())
  }

}