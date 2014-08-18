package themes

import au.id.jazzy.erqx.engine.models._
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
}
