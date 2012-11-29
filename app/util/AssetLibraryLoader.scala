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
  def load(assetLibraryPath: String) = AssetLibrary(
    loadFolder(new File(assetLibraryPath), assetLibraryPath).getOrElse(AssetFolder.Empty),
    assetLibraryPath)

  // is this a valid directory?
  private def isValidDirectory(path: File): Boolean = {
    path.isDirectory && !path.getName.startsWith(".")
  }

  //
  // TODO: refactor into a walk()-style iterator
  //

  /**
   * load a folder of Assets
    */
  private def loadFolder(path: File, basePath: String): Option[AssetFolder] = {
    val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    val assets = allFiles.filter( Asset.isValidAsset(_) )

    if (folders.isEmpty && assets.isEmpty) None
    else Some(AssetFolder(
      name = path.getName,
      assets = assets.map(Asset(_, basePath)).sortBy(_.nameLower),
      folders = folders.flatMap(loadFolder(_, basePath)).sortBy(_.name.toLowerCase)))    
  }

  /**
   * get list of all assets in a folder
   */
  def findAllAssetFiles(path: File): List[File] = {
    val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    val assets = allFiles.filter( Asset.isValidAsset(_) )

    assets ++ folders.flatMap(findAllAssetFiles(_))
  }

  /**
   * get list of all valid asset folders
   */
  def findAllAssetFolders(path: File): List[File] = {

     val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )

    path :: folders.flatMap(findAllAssetFolders(_))
  }
}
