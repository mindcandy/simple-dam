package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import util.{Settings, Archiver}

/* 
 * main controller for Library UI - serve up base pages for AJAX UI
 */
object LibraryUI extends Controller {

  /**
   * main index -- also currently does a search
   */
  def index(search: String, keyword: String, order: String) = Action { implicit request =>

    Ok(views.html.libraryUI(Settings.title, search.trim, "", AssetLibrary.current, None, keyword, order));
  }

  /**
   * search within a folder (must be permalink)
   */
  def listAssetsInFolder(folderPath: String, order: String) = Action { implicit request =>

    Ok(views.html.libraryUI(Settings.title, "", folderPath, AssetLibrary.current, None, "", order));
  }

  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = Action { implicit request =>
    // TODO: CHANGE
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)
    
    Ok(views.html.index(List(), Settings.title, "", "", AssetLibrary.current, Some(asset), "", ""));
  }

  /**
   * serve up previously archived folder
   */
  def downloadFolder(folderPath: String) = Action { 
    if (AssetLibrary.areFolderArchivesGenerated) {
      val archivePath = Archiver.pathToArchiveFolder(folderPath)
      Redirect(routes.FileServer.serveArchive(archivePath))
    } else {
      BadRequest("Folder archives have not been built")
    }
  } 
}
