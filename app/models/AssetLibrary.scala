package models

import play.api._
import util.AssetLibraryLoader


/**
 * a loaded asset library
 * NOTE: put all caching etc in this
 */
case class AssetLibrary (topFolder: AssetFolder) {

  // recurse through all assets
  private def findAssetsRecursively(folder: AssetFolder): List[Asset] = {
    folder.assets ++ folder.folders.flatMap( findAssetsRecursively(_) )
  }

  /**
   * list of all assets -- uncached; prefer sortedAssets
   */
  def allAssets: List[Asset] = findAssetsRecursively(topFolder)

  /**
   * cached SORTED list of all assets - useful for searching/filtering/reversing
   */
  lazy val sortedAssets = allAssets.sortBy(_.nameLower)

  /**
   * find assets that match the given search -- NOTE: very basic at moment! only 1 search term
   */
  def findAsset(search: String) = sortedAssets.filter(_.matches(search))
}

/**
 * a library of assets
 */
object AssetLibrary {

  /**
   * an empty library
   */
  val Empty = AssetLibrary(AssetFolder.Empty)

  /**
   * currently loaded asset library
   */
  var current: AssetLibrary = Empty

  /**
   * load the library from the given path
   */
  def load(path: String): AssetLibrary = {
    Logger.debug("Loading AssetLibrary from " + path)
    val loaded = AssetLibraryLoader.load(path)
    Logger.debug("Load Finished")
    loaded
  }
}