package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import util.{Settings, Archiver}

/* 
 * Asset Library RESTful services for AJAX UI
 */
object LibraryService extends Controller {

  /**
   * endpoint for searching assets
   * searchTypes can be 'folder','keyword', or undefined in which case folder, keyword and name are searched
   */
  def search(searchType: String, search: String, order: String) = Action {

    val sanitisedSearch = search.trim    
    val assets = searchType match {
      case "folder" => AssetLibrary.current.findFolder(sanitisedSearch).allAssets
      case "keyword" => AssetLibrary.current.findAssetsByKeyword(sanitisedSearch)
      case _ if (!sanitisedSearch.isEmpty) => AssetLibrary.current.findAssets(sanitisedSearch)
      case _ => AssetLibrary.current.sortedAssets
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
  def getAsset(assetPath: String) = Action { implicit request =>
    // find asset
    val asset = AssetLibrary.current.findAssetByPath(assetPath)

    val jsAsset = JsObject(Seq(
        "path" -> JsString(asset.original),
        "name" -> JsString(asset.name),
        "hasThumbnail" -> JsBoolean(asset.hasThumbnail),
        "hasPreview" -> JsBoolean(asset.hasPreview),
        "preview" -> JsString(asset.preview),
        "size" -> JsNumber(asset.sizeBytes),
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
