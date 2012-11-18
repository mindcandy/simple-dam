package util

import java.io.File
import models._

/**
 * load assets from a folder
 */
object AssetLibraryLoader {

  /**
   * load asset library from a path
   */
  def load(assetLibraryPath: String) = AssetLibrary(loadFolder(new File(assetLibraryPath), assetLibraryPath))

  // is this a valid directory?
  private def isValidDirectory(path: File): Boolean = {
    path.isDirectory && !path.getName.startsWith(".")
  }

  /**
   * load a folder of Assets
    */
  private def loadFolder(path: File, basePath: String): AssetFolder = {
    val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    val assets = allFiles.filter( Asset.isValidAsset(_) )

    AssetFolder(
      name = path.getName,
      assets = assets.map(Asset(_, basePath)).sortBy(_.nameLower),
      folders = folders.map(loadFolder(_, basePath)).sortBy(_.name.toLowerCase))
  }
}
