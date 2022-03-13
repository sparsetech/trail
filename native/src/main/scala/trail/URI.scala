package trail

import java.net.URLEncoder
import java.io.ByteArrayOutputStream

object URI {
  /** @see http://stackoverflow.com/questions/607176/ */
  def encode(s: String): String =
    URLEncoder.encode(s, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")

  /** Taken from https://android.googlesource.com/platform/libcore/+/adc854b798c1cfe3bfd4c27d68d5cee38ca617da/luni/src/main/java/java/net/URLDecoder.java */
  def decode(s: String): String = {
    val result = new StringBuffer(s.length)
    val out = new ByteArrayOutputStream
    var i = 0

    while (i < s.length) {
      val c = s.charAt(i)
      if (c == '+') result.append(' ')
      else if (c == '%') {
        out.reset()
        while({
          if (i + 2 >= s.length)
            throw new IllegalArgumentException("Incomplete % sequence at: " + i)
          val d1 = Character.digit(s.charAt(i + 1), 16)
          val d2 = Character.digit(s.charAt(i + 2), 16)
          if (d1 == -1 || d2 == -1)
            throw new IllegalArgumentException(
              s"Invalid % sequence (${s.substring(i, i + 3)}) at: ${String.valueOf(i)}.")
          out.write(((d1 << 4) + d2).toByte)
          i += 3
          (i < s.length && s.charAt(i) == '%')
        }) { }
        result.append(out.toString("UTF-8"))
      } else {
        result.append(c)
        i += 1
      }
    }

    result.toString
  }
}
