package models

import play.api._
import java.io.File

/**
 * a library of assets
 */
object AssetLibrary {

  /**
   * an empty folder
   */
  val EmptyFolder = AssetFolder("", List(), List())

  /**
   * the top-most folder, or None if no asset library loaded
   */
  var topFolder = EmptyFolder


  // load the library from the given path
  def load(path: String) {

    // determine if a File is an Asset
    def isAsset(path: File): Boolean = {
      var name = path.getName
      path.isFile &&
        name.contains('.') &&
        !name.startsWith(".") &&
        !name.contains("_thumbnail.") &&
        !name.contains("_preview.") &&
        !name.endsWith(".json")
    }

    def isValidDirectory(path: File): Boolean = {
       path.isDirectory && !path.getName.startsWith(".")
    }

    def find(path: File, suffix: String): Option[File] = {
      val parent = path.getParentFile
      val root = path.getName.substring(0, path.getName.lastIndexOf('.'))
      val newName = root + suffix + ".jpg"
      val file = new File(parent, newName)

      if (file.exists()) {
        Some(file)
      } else {
        None
      }
    }

    // load an Asset
    def loadAsset(path: File): Asset = {
      // do nothing clever yet
      Asset(
        name = path.getName,
        original = path,
        preview = find(path, "_thumbnail"),
        thumbnail = find(path, "_preview"))
    }

    // load a folder of Assets
    def loadFolder(path: File): AssetFolder = {
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

    Logger.debug("Loading AssetLibrary from " + path)
    val loaded = loadFolder(new File(path))
    topFolder = loaded
    Logger.debug("Load Finished")
  }

}