package models

import java.io.{File, FileWriter}
import play.api.libs.json._
import play.api._

/**
 * hold an asset
 */
case class Asset (name: String, original: String, hasThumbnail: Boolean, hasPreview: Boolean, description: String, keywords: Set[String]) {

  def nameLower = name.toLowerCase

  private val matchString = original.toLowerCase + keywords.toList.map(_.toLowerCase).mkString(" ")

  def matches(searchTerms: Seq[String]): Boolean = {
     searchTerms.forall(matchString.contains(_))
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

  val EmptyMetadata = Map[String, String]()

  /**
   * load an Asset
   */
  def apply(path: File, basePath: String): Asset = {
    // load metadata if there is any
    val metadata = loadMetadata(new File(getSuffixPath(path, ".json")))

    val description = metadata.getOrElse("description", "")
    val keywords = convertStringListToSet(metadata.getOrElse("keywords", ""))

    Asset(
      name = path.getName.trim,
      original = path.getPath.replace(basePath,""),
      hasPreview = checkSuffixFileExists(path, ThumbnailSuffix),
      hasThumbnail = checkSuffixFileExists(path, PreviewSuffix),
      description = description,
      keywords = keywords)
  }

  private def loadMetadata (file: File): Map[String, String] = {
    if (file.exists) {
      try {
        val dataFile = scala.io.Source.fromFile(file)
        val parsed = Json.parse(dataFile.mkString)  
        dataFile.close()

        Map(
          "description" -> (parsed \ "description").asOpt[String].getOrElse(""), 
          "keywords" -> (parsed \ "keywords").asOpt[String].getOrElse(""))
      } catch {
        case e => {
          Logger.error("Failure to parse " + file + " error: " + e)
          EmptyMetadata
        }
      }
    } else { 
      EmptyMetadata
    }
  }

  def saveMetadata(basePath: String, asset: Asset, description: String, keywords: String) {
    val metadata = Map("description" -> description, "keywords" -> keywords)
    val json = Json.toJson(metadata)
    val originalPath = new File(basePath + asset.original)
    val metadataPath = getSuffixPath(originalPath, ".json")

    val fw = new FileWriter(metadataPath) 
    fw.write(Json.stringify(json)) 
    fw.close()
  }

  def convertStringListToSet(s: String): Set[String] = s.split(",").map(_.trim).filter(!_.isEmpty).toSet

  /**
   * find file with suffix e.g. passed in foo.pdf and _suffix.jpg would look
   * for foo_suffix.jpg
   */
  private def getSuffixPath (base: File, suffix: String): String = {
    val root = base.getName.substring(0, base.getName.lastIndexOf('.'))
    base.getParentFile + "/" + root + suffix
  }

  private def checkSuffixFileExists (base: File, suffix: String): Boolean = {
    new File(getSuffixPath(base, suffix)).exists
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

