package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import util.Settings

/* 
 * admin mode controller -- allows editing of metadata
 */
object Admin extends Controller with Secured {

    /**
     * utility wrapper of Action to enforce admin mode
     */
    def WithAdmin[A](action: Action[A]): Action[A] = {
        Authenticated(action.parser) { request =>
            if (Settings.isAdmin) {
                action(request)
            } else {
                NotFound
            }
        }
    }


  /**
   * rescan the asset library
   */
  def rescan(url: String) = WithAdmin {
    Authenticated { request =>
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
      if (url.isEmpty)
        Redirect(routes.LibraryUI.index())
      else
        Redirect(url)
    }
  }

  /**
   * edit metadata on an asset
   */
  def editMetadata(asset: String, description: String, keywords: String) = WithAdmin {
    Authenticated { request =>
        // save metadata
        val oldAsset = AssetLibrary.current.findAssetByPath(asset)
        Asset.saveMetadata(AssetLibrary.current.basePath, oldAsset, description, keywords)

        // reload entire library
        AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)

        Ok(Json.toJson(
          Map("status" -> "OK")
        ))
    } 
  }

  /** 
   * mass edit of metadata -- expects a JSON body with parameters (as asset list may be LARGE)
   * { "addKeywords": "foo, bar", "removeKeywords": "foo, bar", "assets": ["asset1", "asset2"] }
   */
  def massEditMetadata = Authenticated(parse.json) { request => {
      if (!Settings.isAdmin) {
        BadRequest(Json.toJson(
          Map("status" -> "FAIL", "message" -> "Admin mode is not enabled")
        ))
      } else {
        // request.body is a JSON obect
        val addKeywords = (request.body \ "addKeywords").as[String]
        val removeKeywords = (request.body \ "removeKeywords").as[String]
        val assets = (request.body \ "assets").as[Seq[String]]

        applyMassEditOfMetadata(assets, addKeywords, removeKeywords)

        Ok(Json.toJson(
          Map("status" -> "OK")
        ))
      }
    }
  }


  private def applyMassEditOfMetadata(assets: Seq[String], addKeywords: String, removeKeywords: String) {
    val addSet = Asset.convertStringListToSet(addKeywords)
    val removeSet = Asset.convertStringListToSet(removeKeywords)

    for (asset <- assets) {
      val oldAsset = AssetLibrary.current.findAssetByPath(asset)
      val updatedKeywords = oldAsset.keywords -- removeSet ++ addSet 

      Asset.saveMetadata(AssetLibrary.current.basePath, oldAsset, oldAsset.description, updatedKeywords.mkString(", "))
    }

    // reload entire library
    AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
  }



}
