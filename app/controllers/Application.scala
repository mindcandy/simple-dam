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
  case class SearchResult(current: Int, total: Int, min: Int, max: Int, assets: Seq[Asset])

  /**
   * main index -- also currently does a search
   */
  def index(search: String, keyword: String, page: Int) = Action { implicit request =>

    // perform search
    val sanitisedSearch = search.trim

    val assets = if (!sanitisedSearch.isEmpty) {
        AssetLibrary.current.findAssets(sanitisedSearch)
      } else if (!keyword.isEmpty) {
        AssetLibrary.current.findAssetsByKeyword(keyword)
      } else {  
        AssetLibrary.current.sortedAssets
      }

    val result = buildResult(assets, page)  

    Ok(views.html.index(result, Settings.title, sanitisedSearch, "", AssetLibrary.current, None, keyword));
  }

  /**
   * search within a folder (must be permalink)
   */
  def findFolder(folderPath: String, page: Int) = Action { implicit request =>

    // find folder
    val folder = AssetLibrary.current.findFolder(folderPath)
    val assets = folder.allAssets
    val result = buildResult(assets, page)

    Ok(views.html.index(result, Settings.title, "", folderPath, AssetLibrary.current, None, ""));
  }

  
  /**
   * show an individual asset's page (must be permalink)
   */
  def showAsset(assetPath: String) = Action { implicit request =>
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)
    val result = buildResult(List(), 1)
    
    Ok(views.html.index(result, Settings.title, "", "", AssetLibrary.current, Some(asset), ""));
  }


  /**
   * build result structure which copes with pagination 
   */
  private def buildResult(assets: List[Asset], page: Int): SearchResult = {

    // actually we don't care about pagination now -- always return everything
    // TODO: clean this up when we definitely won't care about pagination!
    SearchResult(current = 1, total = 1, min = 1, max = 1, assets = assets)

    // val totalPages = 1 + (assets.length / Settings.assetsPerPage)
    // val minVisiblePage = math.max(page - 3, 1)
    // val maxVisiblePage = math.min(minVisiblePage + 6, totalPages)
    
    // // limit response
    // val offset = (page-1) * Settings.assetsPerPage
    // val slice = assets.slice(offset, offset + Settings.assetsPerPage)

    // SearchResult(current = page, total = totalPages, min = minVisiblePage, max = maxVisiblePage, assets = slice)
  }
  
}
