package controllers

import play.api._
import play.api.mvc._

import models._
import util.Settings


object Application extends Controller {

  case class Pagination(current: Int, total: Int, min: Int, max: Int)

  def index(search: String, page: Int) = Action {

    // perform search
    val sanitisedSearch = search.trim
    val assets = if (sanitisedSearch.isEmpty)
      AssetLibrary.current.sortedAssets
    else
      AssetLibrary.current.findAssets(sanitisedSearch)

    showResults(sanitisedSearch, page, "", assets)
  }

  def findFolder(folderPath: String, page: Int) = Action {

    // find folder
    val folder = AssetLibrary.current.findFolder(folderPath)
    val assets = folder.allAssets

    showResults("", page, folderPath, assets)
  }


  private def showResults(sanitisedSearch: String, page: Int, currentFolder: String, assets: List[Asset]) = Action {

    // build pagination info
    val totalPages = 1 + (assets.length / Settings.assetsPerPage)
    val minVisiblePage = math.max(page - 3, 1)
    val maxVisiblePage = math.min(minVisiblePage + 6, totalPages)
    val pagination = Pagination(current = page, total = totalPages, min = minVisiblePage, max = maxVisiblePage)
    
    // limit response
    val offset = (page-1) * Settings.assetsPerPage
    val slice = assets.slice(offset, offset + Settings.assetsPerPage)

    Ok(views.html.index(Settings.title, sanitisedSearch, pagination, currentFolder, slice, AssetLibrary.current));
  }
  
}
