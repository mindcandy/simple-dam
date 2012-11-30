package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.Play.current
import java.util.UUID

import models._
import util.{Settings, Archiver}


/**
 * The archive server co-ordinates creation of zip archives from assets and directs clients to the built zips
 */
object ArchiveBuilder extends Controller {

  /**
   * archive -- take a JSON body that descives which assets in a simple list of paths:
   * {"assets": [ "path1", "path2", "path3"]}
   *
   * It returns JSON object that gives the (relative) url to download the archive from
   * {"status": "OK", "archive": "/path/to/archive.zip"}
   *
   * OR it returns an error:
   * {"status: "FAIL", "message": "there was a failure"}
   */
  def archive = Action(parse.json) { implicit request => {
      val assets = (request.body \ "assets").as[Seq[String]]

      Logger.debug("ArchiveBuilder.archive(" + assets.mkString(", ") + ")")

      val archiveId = UUID.randomUUID.toString.replace("-","")
      val assetRelativeFiles = assets.map(AssetLibrary.current.findAssetByPath(_).original)
      val basePath = AssetLibrary.current.basePath

      // build archive - this can take a while so use a future to avoid blocking
      val archivedPath = Akka.future { 
        Archiver.archiveFiles(archiveId, basePath, assetRelativeFiles)
      }
      Async {
        archivedPath.map(pathResult => pathResult match {
          case Some(pathToArchive) =>  Ok(Json.toJson(
              Map("status" -> "OK", "archive" -> routes.FileServer.serveArchive(pathToArchive).absoluteURL())
            ))

          case None => BadRequest(Json.toJson(
            Map("status" -> "FAIL", 
              "message" -> ("The archive could not be built for assets: " + assets.mkString(", ")))
            ))
        })
      }

    }
  }
}