package controllers

import com.netaporter.uri.Uri
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.decoding.NoopDecoder
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.ws.WSAuthScheme
import play.api.mvc._

import scala.concurrent._

object TestApplication extends Controller {

  private val logger = play.api.Logger.logger

  private val configuration = play.api.Play.current.configuration

  private val config = configuration.underlying
  lazy val authUser = config.getString("conductr-bintray.user")
  lazy val authKey = config.getString("conductr-bintray.key")
  lazy val org = config.getString("conductr-bintray.org")
  lazy val repo = config.getString("conductr-bintray.repo")
  lazy val apiHost = config.getString("conductr-bintray.apiHost")
  lazy val dlHost = config.getString("conductr-bintray.dlHost")

  def home = Action {
    Ok("/proxyFile/:filePath, i.e.: /proxyFile/conductr-1.0.18-systemd.noarch.rpm")
  }

  private def client = global.MyGlobal.customClient

  def proxyFileOriginal(filePath: String) = Action.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._
    client.url(s"https://${dlHost}/${org}/${repo}/${filePath}")
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
          logger.error(s"SNAP ${response.status}")
          BadGateway
        }
    }
  }

  def proxyFile(filePath: String) = Action.async { request =>
    import com.netaporter.uri.dsl._
    import play.api.libs.concurrent.Execution.Implicits._

    val url = s"https://${dlHost}" / org / repo / filePath

    val responseFuture = client.url(url).withAuth(authUser, authKey, WSAuthScheme.BASIC).withFollowRedirects(false).get()
    val futureLocation: Future[String] = responseFuture.map(_.header("Location").get)

    futureLocation.flatMap { location =>
      implicit val c = UriConfig(decoder = NoopDecoder)

      // Use scala-uri so we don't trigger the "illegal character" in java.net.URI
      // for AHC 1.8.x
      //logger.debug(s"location    = $location")
      val locationUri = Uri.parse(location)
      //logger.debug(s"locationUri = $locationUri")

      val request = client.url(locationUri).withAuth(authUser, authKey, WSAuthScheme.BASIC)
      request.getStream().map {
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
            logger.debug(s"SNAP ${response.status}")
            BadGateway
          }
      }
    }
  }

}
