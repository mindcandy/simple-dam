package controllers

import play.api._
import play.api.mvc._

import models._
import util.Settings

/* 
 * main controller 
 */
object Application extends Controller {

  /**
   * utility class to pass pagination info the the View easily
   */
  case class Pagination(current: Int, total: Int, min: Int, max: Int)

  /**
   * main index -- also currently does a search
   */
  def index(search: String, keyword: String, page: Int) = Action {

    // perform search
    val sanitisedSearch = search.trim
    val assets = if (!sanitisedSearch.isEmpty) {
        AssetLibrary.current.findAssets(sanitisedSearch)
      } else if (!keyword.isEmpty) {
        AssetLibrary.current.findAssetsByKeyword(keyword)
      } else {  
        AssetLibrary.current.sortedAssets
      }

    showResults(sanitisedSearch, page, "", keyword, assets)
  }

  /**
   * search within a folder (must be permalink)
   */
  def findFolder(folderPath: String, page: Int) = Action {

    // find folder
    val folder = AssetLibrary.current.findFolder(folderPath)
    val assets = folder.allAssets

    showResults("", page, folderPath, "", assets)
  }

  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = Action {
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)

    val pagination = Pagination(current = 1, total = 1, min = 1, max = 1)
    
    Ok(views.html.index(Settings.title, "", pagination, "", List(), AssetLibrary.current, Some(asset), ""));
  }

  /**
   * Admin mode: rescan the asset library
   */
  def rescan() = Action {
    if (Settings.isAdmin) {
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
      Redirect(routes.Application.index())
    } else {
      NotFound
    }
  }

  private def showResults(sanitisedSearch: String, page: Int, currentFolder: String, keywordSearch: String, assets: List[Asset]) = Action {

    // build pagination info
    val totalPages = 1 + (assets.length / Settings.assetsPerPage)
    val minVisiblePage = math.max(page - 3, 1)
    val maxVisiblePage = math.min(minVisiblePage + 6, totalPages)
    val pagination = Pagination(current = page, total = totalPages, min = minVisiblePage, max = maxVisiblePage)
    
    // limit response
    val offset = (page-1) * Settings.assetsPerPage
    val slice = assets.slice(offset, offset + Settings.assetsPerPage)

    Ok(views.html.index(Settings.title, sanitisedSearch, pagination, currentFolder, slice, AssetLibrary.current, None, keywordSearch));
  }
  
}
