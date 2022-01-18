package funstack.web

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait WebsiteAppConfig extends js.Object {
  def url: String = js.native
}

@js.native
trait AuthAppConfig extends js.Object {
  def url: String         = js.native
  def clientId: String    = js.native
}

@js.native
trait WsAppConfig extends js.Object {
  def url: String                   = js.native
  def allowUnauthenticated: Boolean = js.native
}

@js.native
trait HttpAppConfig extends js.Object {
  def url: String = js.native
}

@js.native
@JSGlobal
object AppConfig extends js.Object {
  def stage: String                      = js.native
  def website: WebsiteAppConfig          = js.native
  def auth: js.UndefOr[AuthAppConfig]    = js.native
  def http: js.UndefOr[HttpAppConfig]    = js.native
  def ws: js.UndefOr[WsAppConfig]        = js.native
  def environment: js.Dictionary[String] = js.native
}
