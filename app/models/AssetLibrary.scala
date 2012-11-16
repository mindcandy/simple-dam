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
      path.isFile && !path.getName.startsWith(".")
    }

    def isValidDirectory(path: File): Boolean = {
       path.isDirectory && !path.getName.startsWith(".")
    }

    // load an Asset
    def loadAsset(path: File): Asset = {
      // do nothing clever yet
      Asset(path.getName, path, None, None)
    }

    // load a folder of Assets
    def loadFolder(path: File): AssetFolder = {
      val allFiles = path.listFiles() match {
        case null => List()
        case files => files.toList
      }

      val folders = allFiles.filter( isValidDirectory(_) )
      val assets = allFiles.filter( isAsset(_) )

      AssetFolder(path.getName, assets.map(loadAsset(_)), folders.map(loadFolder(_)))
    }

    Logger.debug("Loading AssetLibrary from " + path)
    val loaded = loadFolder(new File(path))
    topFolder = loaded
    Logger.debug("Load Finished")
  }

}