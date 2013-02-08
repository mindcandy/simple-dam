package util

import play.api._
import play.api.mvc._

import controllers.Auth

/**
 * authenticated users have a set of groups they are members of
 */
case class AuthenticatedUser(username: String, groups: Set[String]) {

  def toSession: Session = {    
    Session(
      Map(Security.username -> username, 
        AuthenticatedUser.GroupKey -> AuthenticatedUser.encodeGroups(groups) 
        )
      )
  }
}

object AuthenticatedUser {

  def apply(username: String, groups: Seq[String]): AuthenticatedUser = AuthenticatedUser(username, groups.toSet)

  def apply(session: Session): AuthenticatedUser = {
    AuthenticatedUser(
      session.get(Security.username).get, 
      decodeGroups(session.get(GroupKey).getOrElse("")))
  }

  private val GroupKey = "GRP"

  private def encodeGroups(groups: Set[String]): String = rot13(groups.mkString(","))

  private def decodeGroups(encoded: String): Set[String] = rot13(encoded).split(",").toSet

  /** world's worst encryption **/
  private def rot13(plaintext: String) = plaintext map {
    case c if 'a' <= c.toLower && c.toLower <= 'm' => (c + 13).toChar
    case c if 'n' <= c.toLower && c.toLower <= 'z' => (c - 13).toChar
    case c => c
  } 
}
