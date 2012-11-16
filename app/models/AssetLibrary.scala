package models

import play.api._
import util.AssetLibraryLoader


/**
 * a loaded asset library
 * NOTE: put all caching etc in this
 */
case class AssetLibrary (topFolder: AssetFolder) {

  private def findAssetsRecursively(folder: AssetFolder): List[Asset] = {
    folder.assets ++ folder.folders.flatMap( findAssetsRecursively(_) )
  }

  lazy val allAssets: List[Asset] = findAssetsRecursively(topFolder)


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