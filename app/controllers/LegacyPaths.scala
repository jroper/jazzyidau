package controllers

import play.api.mvc.{Action, Controller}

class LegacyPaths extends Controller {

  def redirect(path: String) = Action {
    MovedPermanently("/" + path)
  }
}
