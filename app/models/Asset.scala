package models

import java.io.File

/**
 * hold an asset
 */
case class Asset (name: String, original: String, hasThumbnail: Boolean, hasPreview: Boolean, description: String, keywords: Set[String]) {

  def nameLower = name.toLowerCase

  private val matchString = original.toLowerCase + keywords.toList.map(_.toLowerCase).mkString(" ")

  def matches(search: String): Boolean = {
     matchString.contains(search)
  }

  /**
   * get thumbnail path
   */
  def thumbnail: String = getSuffixPath(hasThumbnail, Asset.ThumbnailSuffix, "")

  /**
   * get preview path
   */
  def preview: String = getSuffixPath(hasPreview, Asset.PreviewSuffix, "")

  /**
   * get folder path
   */
  def folderPath: String = original.substring(0, original.lastIndexOf("/") + 1)


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
    // load metadata if there is any
    val metadataFile = getSuffixPath(path, ".json")
    val description = "Hello, world!"
    val keywords = Set("keywords", "would", "go", "here")    

    Asset(
      name = path.getName.trim,
      original = path.getPath.replace(basePath,""),
      hasPreview = checkSuffixFileExists(path, ThumbnailSuffix),
      hasThumbnail = checkSuffixFileExists(path, PreviewSuffix),
      description = description,
      keywords = keywords)
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
    // TODO: could make regular expression
    path.isFile &&
      name.contains('.') &&
      !name.startsWith(".") &&
      !name.endsWith("_thumbnail.jpg") &&
      !name.endsWith("_preview.jpg") &&
      !name.endsWith(".json")
  }
}

