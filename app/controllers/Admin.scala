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
  def rescan() = WithAdmin {
    Action {
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
      Redirect(routes.Application.index())
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
}