package models

import java.io.File

/**
 * hold an asset
 */
case class Asset (name: String, original: String, hasThumbnail: Boolean, hasPreview: Boolean) {

  lazy val nameLower = name.toLowerCase

  def matches(search: String): Boolean = {
     nameLower.contains(search)
  }
}

