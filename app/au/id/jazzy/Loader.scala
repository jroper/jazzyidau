package au.id.jazzy

import au.id.jazzy.erqx.engine.{BlogComponents, ErqxBuild}
import controllers.{AssetsComponents, LegacyPaths}
import play.api.i18n.I18nComponents
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}
import play.api.ApplicationLoader.Context
import play.api.mvc.{EssentialAction, EssentialFilter}
import play.core.PlayVersion
import play.filters.gzip.GzipFilterComponents
import router.Routes

import scala.concurrent.ExecutionContext

class Loader extends ApplicationLoader {
  def load(context: Context) = {
    new BuiltInComponentsFromContext(context)
      with I18nComponents
      with BlogComponents
      with AssetsComponents
      with GzipFilterComponents {

      lazy val legacyPaths = new LegacyPaths(controllerComponents)
      lazy val router = new Routes(httpErrorHandler, legacyPaths, blogsRouter)

      override lazy val httpFilters = Seq(gzipFilter, new BlabbingFilter())

    }.application
  }
}

class BlabbingFilter(implicit ec: ExecutionContext) extends EssentialFilter {
  private val scalaVersion = scala.util.Properties.scalaPropOrElse("version.number", "unknown")
  private val serverHeader = "Server" -> s"ERQX/${ErqxBuild.version} Play/${PlayVersion.current} Scala/$scalaVersion"

  override def apply(next: EssentialAction) = EssentialAction { rh =>
    next(rh).map(_.withHeaders(serverHeader))
  }
}