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
   * rescan the asset library
   */
  def rescan() = Action {
    if (Settings.isAdmin) {
      AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
      Redirect(routes.Application.index())
    } else {
      NotFound
    }
  }

  /**
   * edit metadata on an asset
   */
  def editMetadata(asset: String, description: String, keywords: String) = Action {
    if (!Settings.isAdmin) {
       NotFound
    } else {

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