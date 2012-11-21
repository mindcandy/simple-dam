package models

import play.api._
import util.AssetLibraryLoader
import scala.collection.SortedSet
import java.util.Date


/**
 * a loaded asset library
 * NOTE: put all caching etc in this
 */
case class AssetLibrary (topFolder: AssetFolder, basePath: String) {

  /**
   * get load time to use as cache bust if needed
   */
  val loadedAt = new java.util.Date().getTime()

  /**
   * cached SORTED list of all assets - useful for searching/filtering/reversing
   */
  lazy val sortedAssets: List[Asset] = topFolder.allAssetsUnsorted.sortBy(_.nameLower)

  /**
   * cached set of all keywords used in all assets
   */
  lazy val keywords: List[String] = collectKeywords(sortedAssets)

  /**
   * find assets that match the given search -- NOTE: very basic at moment! only 1 search term
   */
  def findAssets(search: String): List[Asset] = sortedAssets.filter(_.matches(search.toLowerCase))

  /**
   * find a folder by its path
   */
  def findFolder(path: String): AssetFolder = {

    def findFolderRecursive(folder: AssetFolder, components: Array[String]): AssetFolder = {
      if (components.isEmpty) folder
      else {
        folder.folders.find(_.name == components.head) match {
          case Some(matchingFolder) => findFolderRecursive(matchingFolder, components.tail)
          case None => throw new Exception("No matching folder for " + components.head)
        }
      }
    }

    val pathComponents = path.split("/").filter(!_.isEmpty)
    findFolderRecursive(topFolder, pathComponents)
  } 

  /** 
   * find asset by its path
   */
  def findAssetByPath(assetPath: String): Asset = {

    // split into path and folder
    val splitPoint = assetPath.lastIndexOf("/") + 1
    val path = assetPath.substring(0, splitPoint)
    val assetName = assetPath.substring(splitPoint)

    val folder = findFolder(path)

    folder.assets.find(_.name == assetName).get
  }

  /** 
   * find by keyword
   */
  def findAssetsByKeyword(keyword: String): List[Asset] = {
    sortedAssets.filter(_.keywords.contains(keyword) )
  }

  /**
   * collect all keywords
   */
  private def collectKeywords(assets: Seq[Asset]): List[String] = {
    assets.foldLeft (Set[String]()) {
      case (set, asset) => set ++ asset.keywords
    }.toList.sorted
  }
}

/**
 * a library of assets
 */
object AssetLibrary {

  /**
   * an empty library
   */
  val Empty = AssetLibrary(AssetFolder.Empty, "")

  /**
   * currently loaded asset library
   */
  var current: AssetLibrary = Empty

  /**
   * load the library from the given path
   */
  def load(path: String): AssetLibrary = {
    Logger.debug("Loading AssetLibrary from " + path)
    AssetLibraryLoader.load(path)
  }
}