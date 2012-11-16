package models

import java.io.File

/**
 * hold an asset
 */
case class Asset (name: String, original: File, thumbnail: Option[File], preview: Option[File]) {

  lazy val nameLower = name.toLowerCase

  def matches(search: String): Boolean = {
     nameLower.contains(search)
  }
}

