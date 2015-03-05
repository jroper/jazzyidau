package au.id.jazzy

import au.id.jazzy.erqx.engine.Blogs
import au.id.jazzy.erqx.engine.controllers.BlogsRouter
import controllers.LegacyPaths
import play.api.i18n.I18nComponents
import play.api.libs.concurrent.AkkaComponents
import play.api.{BuiltInComponentsFromContext, ApplicationLoader}
import play.api.ApplicationLoader.Context
import router.Routes

class Loader extends ApplicationLoader {
  def load(context: Context) = {
    new BuiltInComponentsFromContext(context) with AkkaComponents with I18nComponents {

      lazy val blogs: Blogs = new Blogs(configuration, actorSystem)
      lazy val blogsRouter: BlogsRouter = new BlogsRouter(messagesApi, blogs)

      lazy val router = new Routes(httpErrorHandler, new LegacyPaths, blogsRouter)

    }.application
  }
}
