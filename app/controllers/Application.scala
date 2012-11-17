package controllers

import play.api._
import play.api.mvc._

import models._

object Application extends Controller {

  private def folderToList(folder: AssetFolder): String = {

    val foldersList = if (!folder.assets.isEmpty) {
       folder.assets.map{
         case asset => {
           val hasThumb = if (asset.hasThumbnail) " [thumb]" else ""
           val hasPreview = if (asset.hasPreview) " [preview]" else ""
           val route = routes.FileServer.serve(asset.original)

           "<li><a href=\"" + route + "\">" + asset.name + "</a>" + hasThumb + hasPreview + "</li>"
        }
       }.mkString( "<ul>", "", "</ul>")
      } else ""

    val assetsList =
      if (!folder.folders.isEmpty) {
        folder.folders.map(folderToList(_)).mkString( "<ul>", "", "</ul>")
      } else ""

    "<li>" + folder.name + "/" + foldersList + assetsList + "</li>"
  }

  def index = Action {

    val top = AssetLibrary.current.topFolder
    Ok("Found assets <ul>" + folderToList(top) + "</ul>").as(HTML)
    //views.html.index
  }
  
}
