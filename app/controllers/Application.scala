package controllers

import play.api._
import play.api.mvc._

import models._

object Application extends Controller {


  private def emitAsset(asset: Asset): String = {
    val hasThumb =
      if (asset.hasThumbnail)
        " <img src=\"" + routes.FileServer.serve(asset.thumbnail) + "\"/>"
      else ""
    val hasPreview =
      if (asset.hasPreview)
        " <a href=\"" + routes.FileServer.serve(asset.preview) + "\">[preview]</a>"
      else ""
    val route = routes.FileServer.serve(asset.original)

    "<li class=\"asset\"><a href=\"" + route + "\">" + asset.name + "</a>" + hasThumb + hasPreview + "</li>"
  }

  private def folderToList(folder: AssetFolder): String = {

    val foldersList = if (!folder.assets.isEmpty) {
       folder.assets.map(emitAsset(_)).mkString( "<ul>", "", "</ul>")
      } else ""

    val assetsList =
      if (!folder.folders.isEmpty) {
        folder.folders.map(folderToList(_)).mkString( "<ul>", "", "</ul>")
      } else ""

    "<li>" + folder.name + "/" + foldersList + assetsList + "</li>"
  }

  def listAssets(assets: List[Asset]) = {
    "<ul class=\"assets\">" + assets.map(emitAsset(_)).mkString("") + "</ul>"
  }

  // debugging display for now -- proper UI to come later!
  def index(search: String, offset: Int, limit: Int) = Action {

//    val top = AssetLibrary.current.topFolder
//    val result = "Found assets <ul>" + folderToList(top) + "</ul>"

    //views.html.index
    val assets = if (search.isEmpty)
      AssetLibrary.current.sortedAssets
    else
      AssetLibrary.current.findAssets(search)

    // limit response
    val slice = assets.slice(offset, offset + limit)

    val searchInfo = if (search.isEmpty)
      "Showing all assets</p>"
    else
      "<p>Seached for '" + search + "'</p>"

    val next = if (assets.length > (offset + limit))
      "<a href=\"" + routes.Application.index(search, offset + limit, limit) + "\">next</a>"
    else "next"

    val previous = if (offset > 0)
      "<a href=\"" + routes.Application.index(search, math.max(offset - limit,0), limit) + "\">previous</a>"
    else "previous"

    val result = "<p>" + searchInfo + "</p>"+
      "<p>Showing " + offset + "-" + (offset + limit -1) + " of " + assets.length + " results. " +
      previous + "  " + next + "</p>" +
      listAssets(slice)


    Ok(result).as(HTML)
  }
  
}
