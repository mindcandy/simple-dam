package util

import play.api._


/**
 * get settings
 */
object Settings {

  /**
   * get asset library path
   */
  def assetLibraryPath = {
    Play.current.configuration.getString("assetLibrary") match {
      case Some(path) => path
      case None => throw new IllegalArgumentException("No assetLibrary path defined in application.conf")
    }
  }
}
