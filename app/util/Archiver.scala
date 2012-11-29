package util


import java.io.{BufferedReader, FileInputStream, FileOutputStream, File, InputStream, OutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import play.api.Logger


/**
 *  Utility for archiving Assets and folders of Assets (note: filters out non-asset files)
 */
object Archiver {

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
  def archiveFolder(folderPath: String): String = {

    val archivePath = pathToArchiveFolder(folderPath)
    val fullArchivePath = Settings.archiveCachePath + archivePath
    val fullFolderPath = Settings.assetLibraryPath + folderPath

    Logger.debug("ArchiveFolder: " + folderPath + " -> " + archivePath)
    compress(fullArchivePath, 
      AssetLibraryLoader.findAllAssetFiles(new File(fullFolderPath)),
      Settings.assetLibraryPath)

    Logger.debug("done")
    archivePath
  }

  // ensure a directory exists
  private def ensureExists(directory: File) {
    if (!directory.exists) {
      Logger.debug("creating dir: " + directory)
      directory.mkdirs
    }
  }

  // get the relative path for a file
  private def getRelativePath(file: File, basePath: String): String = {
    file.getCanonicalPath.replaceFirst(basePath, "")
  }


  // create the zipfile
  private def compress(zipFilepath: String, files: List[File], basePath: String) {

    // make parent dirs
    ensureExists(new File(zipFilepath).getParentFile)

    Logger.debug("Creating zip: " + zipFilepath)
    val zip = new ZipOutputStream(new FileOutputStream(zipFilepath));
    try {
      for (file <- files) {
        //add zip entry to output stream
        val relativePath = getRelativePath(file, basePath)
        Logger.debug("adding file: " + file + " -> " + relativePath)
        zip.putNextEntry(new ZipEntry(relativePath));

        val in = new FileInputStream(file.getCanonicalPath);
        transferAndClose(in, zip)
        zip.closeEntry();
      }
    }
    finally {
      zip.close();
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