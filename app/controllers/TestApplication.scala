package controllers

import play.api.mvc._
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

import play.api.Application
import play.api.http.{ ContentTypes, HeaderNames }
import play.api.libs.ws.WS
import play.api.libs.ws.WSAuthScheme

class TestApplication extends Controller {
  val config = ConfigFactory.load()
  lazy val authUser = config.getString("conductr-bintray.user")
  lazy val authKey = config.getString("conductr-bintray.key")
  lazy val org = config.getString("conductr-bintray.org")
  lazy val repo = config.getString("conductr-bintray.repo")
  lazy val apiHost = config.getString("conductr-bintray.apiHost")
  lazy val dlHost = config.getString("conductr-bintray.dlHost")

  def home = Action{
    Ok("/proxyFile/:filePath, i.e.: /proxyFile/conductr-1.0.18-systemd.noarch.rpm")
  }
  def proxyFile(filePath: String) = Action.async { request =>

    WS.url(s"https://${dlHost}/${org}/${repo}/${filePath}")
      .withAuth(authUser, authKey, WSAuthScheme.BASIC)
      .withFollowRedirects(true).getStream().map {

      case (response, body) =>
        if (response.status == OK) {
          val contentType = response.headers.get(HeaderNames.CONTENT_TYPE).flatMap(_.headOption)
            .getOrElse(ContentTypes.BINARY)
          response.headers.get(HeaderNames.CONTENT_LENGTH) match {
            case Some(Seq(length)) =>
              Ok.feed(body).as(contentType).withHeaders(HeaderNames.CONTENT_LENGTH -> length)
            case _ =>
              Ok.chunked(body).as(contentType)
          }
        } else {
          System.out.println("SNAP")
          BadGateway
        }
    }
  }

}
