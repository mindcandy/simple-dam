package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.BodyParsers._
import play.api.libs.concurrent.Promise
import play.api.libs.json._

import play.api.libs.ws._

import util.{Settings, AuthenticatedUser}

import views._




object Auth extends Controller {

  /** token to force a relogin for existing logged in users */
  val authToken = "10"
  val authTokenKey = "authToken"


  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  /**
   * get current authenticated user, if auth is enabled
   */
  def currentUser[A] (implicit request: Request[A]): Option[AuthenticatedUser] = {
    if (!authEnabled) 
      None
    else 
      Some(AuthenticatedUser(request.session))
  }

  lazy val authEnabled = Play.current.configuration.getString("application.auth.enabled").getOrElse("false") == "true"

  lazy val authUrl = Play.current.configuration.getString("application.auth.authurl")

  lazy val authInfo = Play.current.configuration.getString("application.auth.infoText").getOrElse("")

  private def processWordpressResponse(username: String, json: JsValue): Option[AuthenticatedUser] = {
    (json \ "auth").asOpt[String] match {
      case Some("true") => Some(AuthenticatedUser(username, (json \ "groups").asOpt[Seq[String]].getOrElse(List()) ))
      case _ => None
    }
  }

  def checkWithWordpress(username: String, password: String): Promise[Option[AuthenticatedUser]] = {
    /*** MOCKING **/
    //Promise.pure(Some(AuthenticatedUser(username, List("mind candy", "moshi monsters") )))
    
    authUrl match {
      case Some(url) => WS.url(url).withQueryString(("username", username), ("password", password)).get().map { 
        response => processWordpressResponse(username, response.json)
      }
      case _ => Promise.pure(None)
    }
  }

  def login = Action { implicit request =>
    if (authEnabled) {
      Ok(html.login(Settings.title, loginForm, authInfo, routes.Auth.authenticate))
    } else {  
      Redirect(routes.LibraryUI.index()).withSession(Security.username -> "user")
    }
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.Auth.login),
      userPass => {
        val (username, password) = userPass
        Async {
          checkWithWordpress(username, password).map { authed =>
            authed match {
              case Some(user) => 
                Redirect(routes.LibraryUI.index()).withSession(user.toSession + (Auth.authTokenKey -> Auth.authToken)) 
              case _ => Redirect(routes.Auth.login)
            }
          }
        }
      }
    )
  }


}

trait Secured {

  case class AuthenticatedRequest[A]( val username: String, request: Request[A] ) extends WrappedRequest(request)

  def Authenticated[A](parser: BodyParser[A])(f: AuthenticatedRequest[A] => Result) = {
    Action(parser) { request => {
        val authTokenOk = request.session.get(Auth.authTokenKey) == Some(Auth.authToken)
        
        request.session.get("username") match {
          case Some(username) if authTokenOk => f(AuthenticatedRequest(username, request))
          case _ => onUnauthorized(request)
        }
      }
    }
  }

  def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent] = {
    Authenticated(parse.anyContent)(f)
  }

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

}

