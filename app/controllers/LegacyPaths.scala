package controllers

import play.api.mvc.{AbstractController, ControllerComponents}

class LegacyPaths(components: ControllerComponents) extends AbstractController(components) {

  def redirect(path: String) = Action {
    MovedPermanently("/" + path)
  }
}
