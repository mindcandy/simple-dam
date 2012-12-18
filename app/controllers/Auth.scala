package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc.BodyParsers._

import util.Settings

import views._

object Auth extends Controller {

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying ("Invalid username or password", result => result match {
      case (username, password) => checkWithWordpress(username, password)
    })
  )

  val wordpressAuthCookie = Play.current.configuration.getString("application.auth.cookiename").get
  val authUrl    = Play.current.configuration.getString("application.auth.authurl").get
  val wpLoginUrl = Play.current.configuration.getString("application.auth.loginurl").get

  def checkWithWordpress(username: String, password: String): Boolean = {
    true
  }

  def login = Action { implicit request =>
    Ok(html.login(Settings.title, loginForm, routes.Auth.authenticate))
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.Auth.login),
      user => Redirect(routes.LibraryUI.index()).withSession (Security.username -> user._1)
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

