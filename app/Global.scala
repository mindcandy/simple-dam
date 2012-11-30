
import play.api._
import play.api.libs.concurrent._
import akka.util.duration._
import play.api.Play.current


import models.AssetLibrary
import util.{Settings, Archiver}
import java.io.File

object Global extends GlobalSettings {

  override def onStart(app:Application) {
    AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)

    if (Settings.archiveOnStart) {
      Akka.system.scheduler.scheduleOnce(10 seconds) {
        Archiver.createAllFolderArchives()
        AssetLibrary.areFolderArchivesGenerated = true 
      }
    }
  }

}

