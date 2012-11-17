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

  /**
   * get thumbnail path
   */
  def thumbnail: String = getSuffixPath(hasThumbnail, Asset.ThumbnailSuffix, "")

  /**
   * get preview path
   */
  def preview: String = getSuffixPath(hasPreview, Asset.PreviewSuffix, "")


  private def getSuffixPath(exists: Boolean, suffix: String, default: String): String = {
    if (exists) Asset.getSuffixPath(new File(original), suffix)
    else default
  }

}


object Asset {

  val ThumbnailSuffix =  "_thumbnail.jpg"
  val PreviewSuffix = "_preview.jpg"

  /**
   * load an Asset
   */
  def apply(path: File, basePath: String): Asset = {
    // do nothing clever yet
    // TODO: load metadata
    //  val metadata = findSuffix(path, ".json")
    Asset(
      name = path.getName.trim,
      original = path.getPath.replace(basePath,""),
      hasPreview = checkSuffixFileExists(path, ThumbnailSuffix),
      hasThumbnail = checkSuffixFileExists(path, PreviewSuffix))
  }

  /**
   * find file with suffix e.g. passed in foo.pdf and _suffix.jpg would look
   * for foo_suffix.jpg
   */
  private def getSuffixPath (base: File, suffix: String): String = {
    val root = base.getName.substring(0, base.getName.lastIndexOf('.'))
    base.getParentFile + "/" + root + suffix
  }

  private def checkSuffixFileExists (base: File, suffix: String): Boolean = {
    new File(getSuffixPath(base, suffix)).exists()
  }

  /**
   * determine if a File is an Asset
   */
  def isValidAsset(path: File): Boolean = {
    var name = path.getName
    path.isFile &&
      name.contains('.') &&
      !name.startsWith(".") &&
      !name.contains("_thumbnail.") &&
      !name.contains("_preview.") &&
      !name.endsWith(".json")
  }
}

