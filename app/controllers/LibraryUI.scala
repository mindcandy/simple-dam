package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import util.{Settings, Archiver, AuthenticatedUser}

/* 
 * main controller for Library UI - serve up base pages for AJAX UI
 */
object LibraryUI extends Controller with Secured with UserAgentCheck {


  private def userView[A](implicit request: Request[A]) = UserView(AssetLibrary.current, Auth.currentUser)

  /**
   * main index -- also currently does a search
   */
  def index(search: String, keyword: String, order: String) = WithCompatibleUserAgent { 
    Authenticated { implicit request =>
      Ok(views.html.libraryUI(Settings.title, search.trim, "", AssetLibrary.current, None, keyword, order, userView));
    } 
  }

  /**
   * search within a folder (must be permalink)
   */
  def listAssetsInFolder(folderPath: String, order: String) = WithCompatibleUserAgent { 
    Authenticated { implicit request =>
      Ok(views.html.libraryUI(Settings.title, "", folderPath, AssetLibrary.current, None, "", order, userView));
    }
  }
  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = WithCompatibleUserAgent { 
    Authenticated { implicit request =>    
      val asset = AssetLibrary.current.findAssetByPath(assetPath)    
      Ok(views.html.libraryUI(Settings.title, "", "", AssetLibrary.current, Some(asset), "", "", userView));
    }
  }

  /**
   * serve up previously archived folder
   */
  def downloadFolder(folderPath: String) = WithCompatibleUserAgent { 
    Authenticated { implicit request =>
      if (AssetLibrary.areFolderArchivesGenerated) {
        val archivePath = Archiver.pathToArchiveFolder(folderPath)
        Redirect(routes.FileServer.serveArchive(archivePath))
      } else {
        BadRequest("Folder archives have not been built")
      }
    }
  } 
}
