package controllers

import play.api._
import play.api.mvc._
//import play.api.libs._
//import play.api.libs.iteratee._

import Play.current

import java.io._
import util.Settings

/**
 * serve up files
 */
object FileServer extends Controller with Secured {

  /**
   * Serve a file from the Asset library 
   */
  def serve(path: String) = {
    at(Settings.assetLibraryPath, path)
  }

  /**
   * serve a generated archive file 
   */
  def serveArchive(path: String) = {
    at(Settings.archiveCachePath, path)
  }

  /**
   * serve a file for the Theme
   * Should probably be one of the following files:
   * favicon.png
   * theme.css
   * icon.png
   */
  def serveTheme(path: String) = {
    if (Settings.themePath.isEmpty) {
      // use internal default theme
      Assets.at("/public/defaultTheme", path)

    } else {
      // serve theme from external path
      at(Settings.themePath, path)
    }
  }


  // pasted from ExternalAssets -- to be replaced with better code!
  private def at(rootPath: String, file: String): Action[AnyContent] = Authenticated { request =>

      val fileToServe = new File(rootPath, file)

      if (fileToServe.exists) {
        Ok.sendFile(fileToServe, inline = true).withHeaders(CACHE_CONTROL -> "max-age=3600")
      } else {
        NotFound
      }
  }
}


