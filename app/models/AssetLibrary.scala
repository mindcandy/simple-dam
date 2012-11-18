package models

import play.api._
import util.AssetLibraryLoader


/**
 * a loaded asset library
 * NOTE: put all caching etc in this
 */
case class AssetLibrary (topFolder: AssetFolder) {

  /**
   * cached SORTED list of all assets - useful for searching/filtering/reversing
   */
  lazy val sortedAssets = topFolder.allAssetsUnsorted.sortBy(_.nameLower)

  /**
   * find assets that match the given search -- NOTE: very basic at moment! only 1 search term
   */
  def findAssets(search: String) = sortedAssets.filter(_.matches(search))

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
    AssetLibraryLoader.load(path)
  }
}