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

  private def processWordpressResponse(username: String, json: JsValue): Either[String,AuthenticatedUser] = {
    Logger.debug(json.toString)
    (json \ "auth").asOpt[Boolean] match {
      case Some(true) => Right(AuthenticatedUser(username, (json \ "groups").asOpt[Seq[String]].getOrElse(List()) ))
      case _ => Left((json \ "error").asOpt[String].getOrElse("ERROR: Failed to authenticate!"))
    }
  }

  def checkWithWordpress(username: String, password: String): Promise[Either[String,AuthenticatedUser]] = {    
    authUrl match {
      case Some(url) => WS.url(url).withQueryString("__api_auth" -> "1", "username" -> username, "password" -> password).get().map { 
        response => processWordpressResponse(username, response.json)
      }
      case _ => Promise.pure(Left("ERROR: No url defined for external authentication! Contact Support!"))
    }
  }

  def showLoginForm(error: String)  = html.login(Settings.title, loginForm, authInfo, routes.Auth.authenticate, error)

  def login = Action { implicit request =>
    if (authEnabled) {
      Ok(showLoginForm(""))
    } else {  
      Redirect(routes.LibraryUI.index()).withSession(Security.username -> "user")
    }
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Ok(showLoginForm("")),
      userPass => {
        val (username, password) = userPass
        Async {
          checkWithWordpress(username, password).map { authed =>
            authed match {
              case Right(user) => 
                Redirect(routes.LibraryUI.index()).withSession(user.toSession + (Auth.authTokenKey -> Auth.authToken)) 
              case Left(error) => 
                Ok(showLoginForm(error))
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

