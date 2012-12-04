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
  def index(search: String, keyword: String, order: String) = Action { implicit request =>

    // perform search
    val sanitisedSearch = search.trim

    val assets = if (!sanitisedSearch.isEmpty) {
        AssetLibrary.current.findAssets(sanitisedSearch)
      } else if (!keyword.isEmpty) {
        AssetLibrary.current.findAssetsByKeyword(keyword)
      } else {  
        AssetLibrary.current.sortedAssets
      }

    val sortedAssets = orderAssets(assets, order)

    Ok(views.html.index(sortedAssets, Settings.title, sanitisedSearch, "", AssetLibrary.current, None, keyword, order));
  }

  /**
   * search within a folder (must be permalink)
   */
  def listAssetsInFolder(folderPath: String, order: String) = Action { implicit request =>

    // find folder
    val folder = AssetLibrary.current.findFolder(folderPath)
    val assets = folder.allAssets
    val sortedAssets = orderAssets(assets, order)

    Ok(views.html.index(sortedAssets, Settings.title, "", folderPath, AssetLibrary.current, None, "", order));
  }

  /**
   * sort assets
   */
  private def orderAssets(assets: Seq[Asset], order: String): Seq[Asset] = {
    order match {
      // order by time (last modified)
      case "time" => assets.sortBy(_.lastModified)
      case "-time" => assets.sortBy(- _.lastModified)

      // order by size
      case "size" => assets.sortBy(_.sizeBytes)
      case "-size" => assets.sortBy(- _.sizeBytes)

      // reverse default order
      case "-name" => assets.reverse
      // default ordering is by name (or unknown ordering)
      case _ => assets
    }
  }

  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = Action { implicit request =>
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
