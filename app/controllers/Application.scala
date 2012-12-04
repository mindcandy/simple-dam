package controllers

import play.api._
import play.api.mvc._

import models._
import util.{Settings, Archiver}

/* 
 * main controller 
 */
object Application extends Controller {

  /**
   * main index -- also currently does a search
   */
  def index(search: String, keyword: String) = Action { implicit request =>

    // perform search
    val sanitisedSearch = search.trim

    val assets = if (!sanitisedSearch.isEmpty) {
        AssetLibrary.current.findAssets(sanitisedSearch)
      } else if (!keyword.isEmpty) {
        AssetLibrary.current.findAssetsByKeyword(keyword)
      } else {  
        AssetLibrary.current.sortedAssets
      }

    Ok(views.html.index(assets, Settings.title, sanitisedSearch, "", AssetLibrary.current, None, keyword));
  }

  /**
   * search within a folder (must be permalink)
   */
  def findFolder(folderPath: String) = Action { implicit request =>

    // find folder
    val folder = AssetLibrary.current.findFolder(folderPath)
    val assets = folder.allAssets

    Ok(views.html.index(assets, Settings.title, "", folderPath, AssetLibrary.current, None, ""));
  }

  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = Action { implicit request =>
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)
    
    Ok(views.html.index(List(), Settings.title, "", "", AssetLibrary.current, Some(asset), ""));
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
