package models

import java.io.{File, FileWriter}
import scala.tools.nsc.io.{File => NscFile}
import play.api.libs.json._
import play.api._
import util.Humanize

/**
 * hold an asset
 */
case class Asset (
  name: String, 
  original: String, 
  hasThumbnail: Boolean, 
  hasPreview: Boolean, 
  description: String, 
  keywords: Set[String],
  sizeBytes: Long,
  lastModified: Long,
  group: Int) {
  
  /*
   * name in Lowercase -- used in sorting a lot
   */
  def nameLower = name.toLowerCase

  // cache the string of useful things to match against - path and keywords
  private val matchString = original.toLowerCase + " " + keywords.toList.map(_.toLowerCase).mkString(" ") + " " + description

  /**
   * check if this asset matches the search terms (ANDed together)
   */
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

  /**
   * get human-readable size
   */
  def humanSize: String = Humanize.filesize(sizeBytes)

  /**
   * get extension of file
   */
  def extension: String = name.substring(name.lastIndexOf('.') + 1)
}


object Asset {

  val ThumbnailSuffix =  "_thumbnail.jpg"
  val PreviewSuffix = "_preview.jpg"

  val EmptyMetadata = Map[String, String]()

  /**
   * load an Asset
   */
  def apply(path: File, basePath: String, group: Int): Asset = {
    val metadataFile = new NscFile(new File(getSuffixPath(path, ".json")))
    val metadata = loadMetadata(metadataFile)

    val description = metadata.getOrElse("description", "")
    val keywords = convertStringListToSet(metadata.getOrElse("keywords", ""))
    val sizeBytes: Long = path.length

    Asset(
      name = path.getName.trim,
      original = path.getPath.replaceFirst(basePath,""),
      hasPreview = checkSuffixFileExists(path, ThumbnailSuffix),
      hasThumbnail = checkSuffixFileExists(path, PreviewSuffix),
      description = description,
      keywords = keywords,
      sizeBytes = sizeBytes,
      lastModified = path.lastModified,
      group = group)
  }

  private def loadMetadata (file: NscFile): Map[String, String] = {
    if (file.exists) {
      try {
        val parsed = Json.parse(file.slurp)

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

  /**
   * save metadata for given file path - will create or overwrite .json files
   */
  def saveMetadata(basePath: String, asset: Asset, description: String, keywords: String) {
    val metadata = Map("description" -> description, "keywords" -> keywords)
    val json = Json.toJson(metadata)
    val originalPath = new File(basePath + asset.original)
    val metadataPath = new File(getSuffixPath(originalPath, ".json"))

    (new NscFile(metadataPath)).writeAll(Json.stringify(json))
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

