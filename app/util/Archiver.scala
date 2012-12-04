package util


import java.io.{BufferedReader, FileInputStream, FileOutputStream, File, InputStream, OutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import play.api.Logger
import java.util.Date

/**
 *  Utility for archiving Assets and folders of Assets (note: filters out non-asset files)
 */
object Archiver {

  /** 
   * archive ALL folders
   */
  def createAllFolderArchives() {
    Logger.info("Starting Archive create at " + new Date())
    val folders = AssetLibraryLoader.findAllAssetFolders(new File(Settings.assetLibraryPath))
    for (folder <- folders) {
      archiveFolder(getRelativePath(folder, Settings.assetLibraryPath))
    }
    Logger.info("Finished Archive create at " + new Date())
  }


  /**
   * generate a path (relative to archiveCachePath) for a given folder archive
   */  
  def pathToArchiveFolder(folderPath: String): String = {
    val folderNoTrailSlash = if (folderPath.endsWith("/")) folderPath.substring(0, folderPath.length-1) else folderPath
    folderNoTrailSlash + ".zip"
  }

  /**
   * archive every Asset in a given folder AND its subfolders
   * NOTE: skips non-asset files
   */
  def archiveFolder(folderPath: String): Option[String] = {

    val archivePath = pathToArchiveFolder(folderPath)
    val fullArchivePath = Settings.archiveCachePath + archivePath
    val fullFolderPath = Settings.assetLibraryPath + folderPath

    val files = AssetLibraryLoader.findAllAssetFiles(new File(fullFolderPath))
    if (files.isEmpty) {
      Logger.debug("No files to archive in: " + folderPath)
      None
    } else {
      Logger.info("ArchiveFolder: " + folderPath)
      if (compress(fullArchivePath, files, Settings.assetLibraryPath)) {
        Some(archivePath)
      } else {
        None
      }
    }
  }

  /**
   * build an archive out of the given files - relative to the base path
   * return a path to the built archive, relative to Settings.archiveCachePath
   */
  def archiveFiles(archiveName:String, basePath: String, files: Seq[String]): Option[String] = {

    val archivePath = "_dynamic/" + archiveName + ".zip"
    val fullArchivePath = Settings.archiveCachePath + archivePath

    if (files.isEmpty) {
      None
    } else {
      val archiveFiles = files.map(new File(basePath, _))
      if (compress(fullArchivePath, archiveFiles, Settings.assetLibraryPath)) {
        Some(archivePath)
      } else {
        None
      }
    }
  }


  // ensure a directory exists
  private def ensureExists(directory: File) {
    if (!directory.exists) {
      directory.mkdirs
    }
  }

  // get the relative path for a file
  private def getRelativePath(file: File, basePath: String): String = {
    file.getCanonicalPath.replaceFirst(basePath, "")
  }


  // create the zipfile
  private def compress(zipFilepath: String, files: Seq[File], basePath: String): Boolean = {

    try 
    {
      // make parent dirs
      ensureExists(new File(zipFilepath).getParentFile)

      // Logger.debug("Creating zip: " + zipFilepath)
      val zip = new ZipOutputStream(new FileOutputStream(zipFilepath));
      try {
        for (file <- files) {
          //add zip entry to output stream
          val relativePath = getRelativePath(file, basePath)
          zip.putNextEntry(new ZipEntry(relativePath));

          val in = new FileInputStream(file.getCanonicalPath);
          transferAndClose(in, zip)
          zip.closeEntry();
        }
      }
      finally {
        zip.close();
      }
      true

    } catch {
      case e: Exception => {
        Logger.error("During zipfile creation for '" + zipFilepath + "' this error happenned: " + e.toString)
        false
      }
    }
  }  

  // from SBT file utilities -- efficiently transfer from input->output stream
  private val BufferSize = 8192
  def transferAndClose(in: InputStream, out: OutputStream): Unit = transferImpl(in, out, true)
  private def transferImpl(in: InputStream, out: OutputStream, close: Boolean)
  {
    try
    {
      val buffer = new Array[Byte](BufferSize)
      def read()
      {
        val byteCount = in.read(buffer)
        if(byteCount >= 0)
        {
          out.write(buffer, 0, byteCount)
          read()
        }
      }
      read()
    }
    finally { if(close) in.close }
  }

}