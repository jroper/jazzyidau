package themes

import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import au.id.jazzy.erqx.engine.models._
import au.id.jazzy.erqx.engine.controllers._
import org.apache.commons.codec.binary.Hex

object Theme extends BlogTheme {
  val name = "my-theme"
  val hash = {
    val erqxTheme = Hex.decodeHex(DefaultTheme.hash.toCharArray)
    val thisTheme = Hex.decodeHex(Build.hash.toCharArray)
    val hashBytes = erqxTheme.zip(thisTheme).map {
      case (a, b) => (a ^ b).asInstanceOf[Byte]
    }
    Hex.encodeHexString(hashBytes)
  }
  
  override def head(blog: Blog, router: BlogReverseRouter, title: Option[String])(implicit req: RequestHeader, messages: Messages): Html =
    views.html.head(blog, router, title)
}
