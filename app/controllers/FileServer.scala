package controllers

import play.api._
import util.Settings

/**
 * serve up files
 */
object FileServer {

  def serve(path: String) = {
    Logger.debug("serving file " + Settings.assetLibraryPath + path)
    controllers.ExternalAssets.at(Settings.assetLibraryPath, path)
  }

}


