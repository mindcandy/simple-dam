package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.BodyParsers._
import play.api.libs.concurrent.Promise

import play.api.libs.ws._

import util.Settings

import views._

object Auth extends Controller {

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  val authEnabled = Play.current.configuration.getString("application.auth.enabled").getOrElse("false")

  val authUrl    = Play.current.configuration.getString("application.auth.authurl")

  def checkWithWordpress(username: String, password: String): Promise[String] = {
    authUrl match {
      case Some(url) => WS.url(url).withQueryString(("username", username), ("password", password)).get().map { response =>
        (response.json \ "auth").as[String]
      }
      case None => Promise.pure("false")
    }
  }

  def login = Action { implicit request =>
    authEnabled match {
      case "true" => Ok(html.login(Settings.title, loginForm, routes.Auth.authenticate))
      case _ => Redirect(routes.LibraryUI.index()).withSession(Security.username -> "user")
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
              case "true" => 
                Redirect(routes.LibraryUI.index()).withSession(Security.username -> username)
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
    Action(parser) { request =>
      request.session.get("username").flatMap( (u: String) => Some(u) ).map { username =>
        f(AuthenticatedRequest(username, request))
      }.getOrElse(onUnauthorized(request))
    }
  }

  def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent] = {
    Authenticated(parse.anyContent)(f)
  }

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

}

