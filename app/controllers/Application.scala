package controllers

import play.api.mvc.{ Action, Controller }

/** It's a single-page app, so not much to see here **/
object Application extends Controller with Secured {
  
  /** Returns the index HTML page for logged-in users **/
  def index(docId: Option[Int]) = withAuth { username => implicit request => Ok(views.html.index(username, docId.getOrElse(1))) }

}