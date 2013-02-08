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
  def load(assetLibraryPath: String) = {

    val path = new File(assetLibraryPath)
    val basePath = assetLibraryPath

     val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    
    if (folders.isEmpty) {
      // nothing to see here
      AssetLibrary(AssetFolder.Empty, assetLibraryPath, Map())

    } else {
      // assign group ids
      val groups = { 
          for ((folder, index) <- folders.zipWithIndex) 
            yield (folder.getName, index) 
        }.toMap

      val subFolders = folders.flatMap { 
          f => loadFolder(f, basePath, groups(f.getName)) 
        }.sortBy(_.name.toLowerCase)

      AssetLibrary(AssetFolder(path.getName, List(), subFolders, 0),
        assetLibraryPath,  groups)
    } 
  }


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
  private def loadFolder(path: File, basePath: String, group: Int): Option[AssetFolder] = {
    val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    val assets = allFiles.filter( Asset.isValidAsset(_) )

    if (folders.isEmpty && assets.isEmpty) {
      // nothing to see here
      None
    } else {
      val subFolders = folders.flatMap(loadFolder(_, basePath, group))

      Some(AssetFolder(
        name = path.getName,
        assets = assets.map(Asset(_, basePath, group)).sortBy(_.nameLower),
        folders = subFolders.sortBy(_.name.toLowerCase),
        group = group))
    }
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
