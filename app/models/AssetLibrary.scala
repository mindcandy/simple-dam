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

    // is this a valid directory?
    def isValidDirectory(path: File): Boolean = {
       path.isDirectory && !path.getName.startsWith(".")
    }

    /**
     * find file with suffix e.g. passed in foo.pdf and _suffix.jpg would look
     * for foo_suffix.jpg
      */
    def findSuffix(base: File, suffix: String): Option[File] = {
      val root = base.getName.substring(0, base.getName.lastIndexOf('.'))
      val file = new File(base.getParentFile, root + suffix)

      if (file.exists()) {
        Some(file)
      } else {
        None
      }
    }

    // load an Asset
    def loadAsset(path: File): Asset = {
      // do nothing clever yet
      // TODO: load metadata
      //  val metadata = findSuffix(path, ".json")
      Asset(
        name = path.getName,
        original = path,
        preview = findSuffix(path, "_thumbnail.jpg"),
        thumbnail = findSuffix(path, "_preview.jpg"))
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