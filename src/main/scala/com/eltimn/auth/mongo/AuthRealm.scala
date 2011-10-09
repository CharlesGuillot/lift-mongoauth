package com.eltimn
package auth.mongo

import org.apache.shiro._
import authc._
import authc.credential.CredentialsMatcher
import authz.{AuthorizationInfo, SimpleAuthorizationInfo}
import realm.AuthorizingRealm
import subject.PrincipalCollection
import org.apache.shiro.util.Nameable

import org.bson.types.ObjectId

import net.liftweb._
import common._
import http.{Factory, RequestVar, S}
import mongodb.record.MongoRecord
import sitemap._
import net.liftweb.util.Helpers

//import AuthLocs._


object AuthRules extends Factory {
  /* config */
  val authUserMeta = new FactoryMaker[AuthUserMeta](SimpleUser) {}
  //val authTokenMeta = new FactoryMaker[AuthTokenMeta[]](SimpleAuthToken) {}
  //val authAfterAuthToken = new FactoryMaker[String]("/set-password") {} // where to send user after logging in with an AuthToken

  //def menus: List[Menu] = List(shiro.sitemap.Locs.logoutMenu)
}

class AuthRealm extends AuthorizingRealm with Loggable {

  private lazy val userMeta = AuthRules.authUserMeta.vend

  def doGetAuthenticationInfo(token: AuthenticationToken): AuthenticationInfo = token match {
    case t: UsernamePasswordToken =>
      val login = Box !! t.getPrincipal openOr
        (throw new AccountException("Null usernames are not allowed by this realm."))

      userMeta.findAuthenticatioInfo(login, getName) match {
        case Full(authInfo) => authInfo
        case Failure(msg, _, _) => throw new AccountException(msg)
        case Empty => throw new UnknownAccountException("No account found for login [" + login.toString + "]")
      }
    case t: UserIdToken => t.authInfo(getName)
    case _ => throw new AccountException("Invalid AuthenticationToken")
  }

  def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
    val principal = getAvailablePrincipal(principals)

    userMeta.findAuthorizationInfo(principal) match {
      case Full(authInfo) => authInfo
      case Failure(msg, _, _) => throw new AccountException(msg)
      case Empty => throw new UnknownAccountException("No account found for principal [" + principal.toString + "]")
    }
  }

  override def onLogout(principals: PrincipalCollection) {
    S.request.foreach(_.request.session.terminate)
    super.onLogout(principals)
  }
}

class BCryptCredentialsMatcher extends CredentialsMatcher {
  import AuthUtil._

  def doCredentialsMatch(token: AuthenticationToken, info: AuthenticationInfo): Boolean = {
    (for {
      upToken <- tryo(token.asInstanceOf[UsernamePasswordToken])
      encryptedPassword <- tryo(info.getCredentials.asInstanceOf[String])
      toTest <- tryo(upToken.getPassword) // Array[Char]
    } yield PasswordField.isMatch(toTest.mkString, encryptedPassword)).openOr(false)
  }
}


object AuthUtil {
  def tryo[T](f: => T): Box[T] = {
    try {
      f match {
        case null => Empty
        case x => Full(x)
      }
    } catch {
      case e => Failure(e.getMessage, Full(e), Empty)
    }
  }
}

/*
 * This puts the userId into the Auth info directly.
 * May be used with a "secret token" to autmatically log users in.
 */
class UserIdToken(userId: AnyRef) extends AuthenticationToken {
  def getPrincipal = userId
  def getCredentials = ""

  def authInfo(realmName: String) = new SimpleAuthenticationInfo(userId, "", realmName)
}