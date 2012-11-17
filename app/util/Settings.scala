package util

import play.api._


/**
 * get settings
 */
object Settings {

  private def config = Play.current.configuration

  /**
   * get asset library path
   */
  def assetLibraryPath: String = config.getString("assetLibrary") match {
      case Some(path) => path
      case None => throw new IllegalArgumentException("No assetLibrary path defined in application.conf")
    }

  def title: String = config.getString("ui.title").getOrElse("Assets")

  def assetsPerPage: Int = config.getInt("ui.assetsPerPage").getOrElse(50)

}
