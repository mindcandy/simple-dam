package controllers

import play.api._
import play.api.mvc._

import models._
import util.Settings

/* 
 * admin mode controller -- allows editing of metadata
 */
object Admin extends Controller {

    /**
     * utility wrapper of Action to enforce admin mode
     */
    def WithAdmin[A](action: Action[A]): Action[A] = {
        Action(action.parser) { request =>
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
    Action {
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
      if (url.isEmpty)
        Redirect(routes.Application.index())
      else
        Redirect(url)
    }
  }

  /**
   * edit metadata on an asset
   */
  def editMetadata(asset: String, description: String, keywords: String) = WithAdmin {
    Action {
        // save metadata
        val oldAsset = AssetLibrary.current.findAssetByPath(asset)
        Asset.saveMetadata(AssetLibrary.current.basePath, oldAsset, description, keywords)

        // reload entire library
        AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)

        // redirect to same page we were on before
        Redirect(routes.Application.showAsset(oldAsset.original))
    } 
  }

  def massEditMetadata(addKeywords: String, removeKeywords: String, redirect: String) = WithAdmin {
    Action { request =>
      // parse the query string ourselves as router doesn't quite manage it in play 2.0.4
      val assets = request.queryString.get("assets").get.toList

      val addSet = Asset.convertStringListToSet(addKeywords)
      val removeSet = Asset.convertStringListToSet(removeKeywords)

      for (asset <- assets) {
        val oldAsset = AssetLibrary.current.findAssetByPath(asset)
        val updatedKeywords = oldAsset.keywords -- removeSet ++ addSet 

        Asset.saveMetadata(AssetLibrary.current.basePath, oldAsset, oldAsset.description, updatedKeywords.mkString(", "))
      }

      // reload entire library
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)

      Redirect(redirect)
    }
  }


}