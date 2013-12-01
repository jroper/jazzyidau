package controllers

import play.api.mvc.{Action, Controller}

object LegacyPaths extends Controller {

  def redirect(path: String) = Action {
    MovedPermanently("/" + path)
  }
}
