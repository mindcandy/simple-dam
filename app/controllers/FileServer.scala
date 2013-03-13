package controllers

import play.api._
import play.api.mvc._
import Play.current
import org.joda.time.format.{ DateTimeFormatter, DateTimeFormat }
import org.joda.time.DateTimeZone
import java.io._
import collection.JavaConverters._

import util.Settings


/**
 * serve up files
 */
object FileServer extends Controller with Secured {

  /**
   * Serve a file from the Asset library -- will be DOWNLOADED by web browser
   */
  def downloadFile(path: String) = {
    at(Settings.assetLibraryPath, path, inBrowser = false)
  }

  /**
     * Serve an image file from the Asset library -- will be shown *in* web browser
     */
    def serveImage(path: String) = {
      at(Settings.assetLibraryPath, path)
    }



  /**
   * serve a generated archive file 
   */
  def serveArchive(path: String) = {
    at(Settings.archiveCachePath, path)
  }

  /**
   * serve a file for the Theme
   * Should probably be one of the following files:
   * favicon.png
   * theme.css
   * icon.png
   */
  def serveTheme(path: String) = {
    if (Settings.themePath.isEmpty) {
      // use internal default theme
      Assets.at("/public/defaultTheme", path)

    } else {
      // serve theme from external path
      at(Settings.themePath, path)
    }
  }


  /********
   * some datetime parsing stuff taken from Play framework's Assets.at()
   */
  private val timeZoneCode = "GMT"

  //Dateformatter is immutable and threadsafe
  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  //Dateformatter is immutable and threadsafe    
  private val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  private val parsableTimezoneCode = " " + timeZoneCode

  def parseDate(date: String): Option[java.util.Date] = try {
    //jodatime does not parse timezones, so we handle that manually
    val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).toDate
    Some(d)
  } catch {
    case _ => None
  }


  
  /**
   * hold file caching info
   */
  case class FileCacheInfo(etag: String, lastModified: String)

  private def cacheInfoForFile(file: File): FileCacheInfo = {

    // using last-modified-time as entity tag for now, so we can seamlessly replace with full etags later if needed
    val modified = lastModifiedForFile(file)
    val etag = modified.toString  
    val lastModified = df.print({ new java.util.Date(modified).getTime })
    FileCacheInfo(etag, lastModified)
  }

  // cache file last-modified time (if not in Admin mode)
  private val lastModifiedCache = (new java.util.concurrent.ConcurrentHashMap[String, Long]()).asScala

  private def lastModifiedForFile(file: File): Long = {
    lastModifiedCache.get(file.getPath).filter(_ => (!Settings.isAdmin && Play.isProd)).orElse {
      val modified = file.lastModified
      lastModifiedCache.put(file.getPath, modified)
      Some(modified)
    }.get
  }

  /**
   * get cache control string -- customisable by setting assets.defaultCache= in the config file
   */
  private val cacheControl: String = Play.mode match {
      case Mode.Prod => (Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600") + " must-revalidate")
    case _ => "no-cache"
  }



  /**
   * This code is adapted from Play's Assets.at() function
   */
  private def at(rootPath: String, file: String, inBrowser: Boolean = true): Action[AnyContent] = Authenticated { request =>

    val fileToServe = new File(rootPath, file)

    if (!fileToServe.exists) {
      NotFound
    } else {
      val info = cacheInfoForFile(fileToServe)

      // responds to Entity Tag If-None-Match query
      request.headers.get(IF_NONE_MATCH).flatMap { ifNoneMatch =>
        Some(info.etag).filter(_ == ifNoneMatch)
      }.map(_ => NotModified).getOrElse {

        // respond to If-Modified-Since query via last modified time
        request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).flatMap { ifModifiedSince =>
          Some(info.lastModified).flatMap(parseDate).filterNot(lastModified => lastModified.after(ifModifiedSince))
        }.map(_ => NotModified.withHeaders(
          DATE -> df.print({ new java.util.Date }.getTime))).getOrElse {

          // send the file, but with caching details
          val response = Ok.sendFile(fileToServe, inline = inBrowser).withHeaders(
            ETAG -> info.etag, LAST_MODIFIED -> info.lastModified, CACHE_CONTROL-> cacheControl)

          if (!inBrowser) {
            // enforce binary download type and quote Content-Disposition for Firefox
            response.withHeaders(
              CONTENT_TYPE -> play.api.http.ContentTypes.BINARY,
              CONTENT_DISPOSITION -> ("attachment; filename=\"" + fileToServe.getName + "\"")  )
          } else {
            response
          }
        }
      }    
    }
  }
}


