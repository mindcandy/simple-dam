package util

import play.api._


/**
 * get settings
 */
object Settings {

  private def config = Play.current.configuration

  private def ensureEndsWithSlash(path: String): String = {
    if (path.endsWith("/")) path
    else path + "/"
  }

  /**
   * get asset library path
   */
  lazy val assetLibraryPath: String = config.getString("assetLibrary") match {
      case Some(path) => ensureEndsWithSlash(path)
      case None => throw new IllegalArgumentException("No assetLibrary path defined in application.conf")
    }

  /**
   * get path to cache .zip files in
   */
  lazy val cachePath: String = config.getString("cachePath") match {
      case Some(path) => ensureEndsWithSlash(path)
      case None => throw new IllegalArgumentException("No cachePath path defined in application.conf")
    }

  /** 
   * get theme path or an empty path
   */
  lazy val themePath: String = config.getString("ui.themePath") match {
      case Some(path) => ensureEndsWithSlash(path)
      case None => ""
    }

  lazy val archiveCachePath: String = cachePath + "zips/"

  lazy val title: String = config.getString("ui.title").getOrElse("Assets")

  lazy val isAdmin: Boolean = config.getBoolean("ui.adminMode").getOrElse(false)

  lazy val archiveOnStart: Boolean = config.getBoolean("archiveOnStart").getOrElse(false)
}
