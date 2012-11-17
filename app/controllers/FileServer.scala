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
object FileServer extends Controller {

  def serve(path: String) = {
    //Logger.debug("serving file " + Settings.assetLibraryPath + path)
    at(Settings.assetLibraryPath, path)
  }


  // pasted from ExternalAssets -- to be replaced with better code!
  private def at(rootPath: String, file: String): Action[AnyContent] = Action { request =>

      val fileToServe = new File(rootPath, file)

      if (fileToServe.exists) {
        Ok.sendFile(fileToServe, inline = true).withHeaders(CACHE_CONTROL -> "max-age=3600")
      } else {
        NotFound
      }
  }
}


