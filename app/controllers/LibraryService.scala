package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import util.{Settings, Archiver, Humanize, AuthenticatedUser}

/* 
 * Asset Library RESTful services for AJAX UI
 */
object LibraryService extends Controller with Secured {

  private def findByKeyword(userView: UserView, sanitisedSearch: String) = {
    if (userView.isEverythingVisible)
      AssetLibrary.current.findAssetsByKeyword(sanitisedSearch)
    else {
      // search in allowed top level folders, then combine searches        
      userView.topFolders.foldLeft (List[Asset]()) {
        case (accum, folder) => accum ++ folder.findAssetsByKeyword(sanitisedSearch)
      }
    }
  }

  private def findString(userView: UserView, sanitisedSearch: String) = {
    if (userView.isEverythingVisible)
      AssetLibrary.current.findAssets(sanitisedSearch)
    else {
      // search in allowed top level folders, then combine searches    
       userView.topFolders.foldLeft (List[Asset]()) {
        case (accum, folder) => accum ++ folder.findAssets(sanitisedSearch)
      }
    }
  }

  private def findAllAssets(userView: UserView) = {
    if (userView.isEverythingVisible)
      AssetLibrary.current.sortedAssets
    else {
      // search in allowed top level folders, then combine searches    
      userView.topFolders.foldLeft (List[Asset]()) {
        case (accum, folder) => accum ++ folder.allAssets
      }
    }
  }

  /**
   * endpoint for searching assets
   * searchTypes can be 'folder','keyword', or undefined in which case folder, keyword and name are searched
   */
  def search(searchType: String, search: String, order: String) = Authenticated { implicit request =>

    val userView = UserView(AssetLibrary.current, Auth.currentUser)
    val sanitisedSearch = search.trim    

    val assets = searchType match {
      case "folder" => AssetLibrary.current.findFolder(sanitisedSearch).allAssets
      case "keyword" => findByKeyword(userView, sanitisedSearch)
      case _ if (!sanitisedSearch.isEmpty) => findString(userView, sanitisedSearch)
      case _ => findAllAssets(userView)
    }
    // TODO: sort on client
    val sortedAssets = orderAssets(assets, order)

    val assetList = Json.toJson(sortedAssets.map { case asset => Map(
          "path" -> asset.original,
          "thum" -> asset.hasThumbnail.toString,
          "size" -> asset.sizeBytes.toString,
          "time" -> asset.lastModified.toString
        ) } )

    val result = JsObject(Seq(
      "status" -> JsString("OK"), 
      "assets" -> assetList,
      "libraryLoadTime" -> JsNumber(AssetLibrary.current.loadedAt)
      ))
    Ok(result)
  }

  /**
   * get an individual asset's data
   */
  def getAsset(assetPath: String) = Authenticated { implicit request =>
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)

    val jsAsset = JsObject(Seq(
        "path" -> JsString(asset.original),
        "name" -> JsString(asset.name),
        "hasThumbnail" -> JsBoolean(asset.hasThumbnail),
        "hasPreview" -> JsBoolean(asset.hasPreview),
        "preview" -> JsString(asset.preview),
        "size" -> JsString(Humanize.filesize(asset.sizeBytes)),
        "time" -> JsNumber(asset.lastModified),
        "description" -> JsString(asset.description),
        "keywords" -> JsArray(asset.keywords.map(JsString(_)).toSeq)
      ))

    Ok(JsObject(Seq(
      "status" -> JsString("OK"), 
      "asset" -> jsAsset
    )))
  }


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
}
