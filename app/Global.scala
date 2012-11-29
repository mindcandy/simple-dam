
import play.api._

import models.AssetLibrary
import util.{Settings, Archiver}
import java.io.File

object Global extends GlobalSettings {

  override def onStart(app:Application) {
    AssetLibrary.current = AssetLibrary.load(Settings.assetLibraryPath)
    if (Settings.archiveOnStart) {
      Archiver.createAllFolderArchives()
    }
  }

}

