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
   * Serve a file from the Asset library -- will be DOWNLOADED by web browser
   */
  def downloadFile(path: String) = {
    at(Settings.assetLibraryPath, path, inBrowser = false)
  }

  /**
     * Serve an image file from the Asset library -- will be shown *in* web browser
     */
    def serveImage(path: String) = {
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
  private def at(rootPath: String, file: String, inBrowser: Boolean = true): Action[AnyContent] = Authenticated { request =>

      val fileToServe = new File(rootPath, file)

      if (fileToServe.exists) {
        Ok.sendFile(fileToServe, inline = inBrowser).withHeaders(CACHE_CONTROL -> "max-age=3600")
      } else {
        NotFound
      }
  }
}


