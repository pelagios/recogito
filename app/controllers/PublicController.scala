package controllers

import play.api.mvc.{ Action, Controller }

object PublicController extends Controller {
  
  def map(id: Int) = Action {
    Ok(views.html.map_public(id))
  }
  
  def api(id: Int) = Action {
    Ok("")
  }

}