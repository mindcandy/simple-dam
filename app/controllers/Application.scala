package controllers

import play.api._
import play.api.mvc._

import models._

object Application extends Controller {

  private def folderToList(folder: AssetFolder): String = {

    val foldersList = if (!folder.assets.isEmpty) {
       folder.assets.map{
         case asset => {
           val hasThumb = if (asset.thumbnail.isDefined) " [thumb]" else ""
           val hasPreview = if (asset.preview.isDefined) " [preview]" else ""

           "<li>" + asset.name + hasThumb + hasPreview + "</li>"
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
