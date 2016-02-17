package global

import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient
import play.api.{Application, GlobalSettings}

/**
 * Adds a custom WS Client with raw URL.
 */
object MyGlobal extends GlobalSettings {

  private var clientVar: Option[WSClient] = None

  def customClient: WSClient = clientVar.get

  override def onStart(app: play.api.Application) = {
    val template = wsClientTemplateConfig(app)

    val builder = new AsyncHttpClientConfig.Builder(template)
    builder.setUseRawUrl(true) // stop Ning from fiddling with it...
    val config = builder.build()

    clientVar = Some(new NingWSClient(config))
  }

  private def wsClientTemplateConfig(app: play.api.Application): AsyncHttpClientConfig = {
    import play.api.libs.ws._
    import play.api.libs.ws.ning._

    val classLoader = app.classloader
    val parser = new DefaultWSConfigParser(configuration, classLoader)
    val builder = new NingAsyncHttpClientConfigBuilder(parser.parse())
    builder.build()
  }

  override def onStop(app: Application): Unit = {
    super.onStop(app)

    clientVar.foreach { client =>
      client.underlying[com.ning.http.client.AsyncHttpClient].close()
    }
  }
}
