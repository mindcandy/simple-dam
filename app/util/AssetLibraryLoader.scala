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
  def load(assetLibraryPath: String) = AssetLibrary(loadFolder(new File(assetLibraryPath)))

  // determine if a File is an Asset
  private def isAsset(path: File): Boolean = {
    var name = path.getName
    path.isFile &&
      name.contains('.') &&
      !name.startsWith(".") &&
      !name.contains("_thumbnail.") &&
      !name.contains("_preview.") &&
      !name.endsWith(".json")
  }

  // is this a valid directory?
  private def isValidDirectory(path: File): Boolean = {
    path.isDirectory && !path.getName.startsWith(".")
  }

  /**
   * find file with suffix e.g. passed in foo.pdf and _suffix.jpg would look
   * for foo_suffix.jpg
   */
  private def findSuffix(base: File, suffix: String): Option[File] = {
    val root = base.getName.substring(0, base.getName.lastIndexOf('.'))
    val file = new File(base.getParentFile, root + suffix)

    if (file.exists()) {
      Some(file)
    } else {
      None
    }
  }

  /**
   * load an Asset
   */
  private def loadAsset(path: File): Asset = {
    // do nothing clever yet
    // TODO: load metadata
    //  val metadata = findSuffix(path, ".json")
    Asset(
      name = path.getName.trim,
      original = path,
      preview = findSuffix(path, "_thumbnail.jpg"),
      thumbnail = findSuffix(path, "_preview.jpg"))
  }

  /**
   * load a folder of Assets
    */
  private def loadFolder(path: File): AssetFolder = {
    val allFiles = path.listFiles() match {
      case null => List()
      case files => files.toList
    }

    val folders = allFiles.filter( isValidDirectory(_) )
    val assets = allFiles.filter( isAsset(_) )

    AssetFolder(
      name = path.getName,
      assets = assets.map(loadAsset(_)),
      folders = folders.map(loadFolder(_)))
  }
}
