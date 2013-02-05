package controllers

import play.api._
import play.api.mvc._

object UserAgentCheck {

  /**
   * minimum version of IE we support
   */
  val minimumInternetExplorerVersion = 9

  /**
   * regular expression to match MSIE versions
   */
  private val InternetExplorerUserAgent = """.*MSIE\s+(\d+)\.(\d+).*""".r

  /**
   * Check the user agent string is valid
   */
  def Check(userAgentOption: Option[String]): Boolean = userAgentOption match {
    case None => true
    case Some(userAgent) => {
      //Logger.trace("checking " + userAgent)
      userAgent match {
        case InternetExplorerUserAgent(major, minor) => (major.toInt >= minimumInternetExplorerVersion)
        case _ => true
      }
    }
  }

} 


trait UserAgentCheck {

  def WithCompatibleUserAgent (f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action { request => {
        if (UserAgentCheck.Check(request.headers.get("User-Agent"))) {
          f(request)  
        } else {
          Results.Ok(views.html.incompatibleBrowser(UserAgentCheck.minimumInternetExplorerVersion))
        }
      }
    }
  }
}


