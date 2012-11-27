package util

object Humanize {

  /**
   * format a size in bytes into a 'human' file size, e.g. bytes, KB, MB, GB, TB, PB
   * Note that bytes/KB will be reported in whole numbers but MB and above will have greater precision
   * e.g. 1 byte, 43 bytes, 443 KB, 4.3 MB, 4.43 GB, etc
   */
  def filesize(size: Long) : String = {
    if (size == 1)
      "1 byte"

    else {
      val suffixes = List(("bytes", 0), ("KB",0), ("MB",1), ("GB",2), ("TB",2), ("PB",2))
      var reducedSize = size.toFloat
      var suffixIndex = 0

      while (reducedSize > 1024.0f)
      {
        suffixIndex += 1
        reducedSize /= 1024.0f
      }

      val precision = suffixes(suffixIndex)._2
      val suffix = suffixes(suffixIndex)._1

      val formatString = "%." + precision.toString + "f " + suffix

      formatString.format(reducedSize)
    }
  }
}