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
  lazy val assetLibraryPath: String = config.getString("assetLibrary") match {
      case Some(path) => path
      case None => throw new IllegalArgumentException("No assetLibrary path defined in application.conf")
    }

  lazy val title: String = config.getString("ui.title").getOrElse("Assets")

  lazy val isAdmin: Boolean = config.getBoolean("ui.adminMode").getOrElse(false);

  // TODO: more configuration e.g. logo, favicon?
}
