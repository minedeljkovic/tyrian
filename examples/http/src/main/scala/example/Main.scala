package example

import cats.syntax.either._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.scalajs._
import org.scalajs.dom.document
import tyrian.Html._
import tyrian._
import tyrian.debugger.Debugger
import tyrian.debugger.DebuggerOptions
import tyrian.debugger.DebuggerSerialize
import tyrian.debugger.ShowCompact
import tyrian.http._

object Main:

  def init: (Model, Cmd[Msg]) =
    (Model("cats", "waiting.gif"), HttpHelper.getRandomGif("cats"))

  def update(msg: Msg, model: Model): (Model, Cmd[Msg]) =
    msg match
      case Msg.MorePlease     => (model, HttpHelper.getRandomGif(model.topic))
      case Msg.NewGif(newUrl) => (model.copy(gifUrl = newUrl), Cmd.Empty)
      case Msg.GifError(_)    => (model, Cmd.Empty)

  def view(model: Model): Html[Msg] =
    div()(
      h2()(text(model.topic)),
      button(onClick(Msg.MorePlease))(text("more please")),
      br,
      img(src(model.gifUrl))
    )

  def subscriptions(model: Model): Sub[Msg] =
    Sub.Empty

  def main(args: Array[String]): Unit =
    Debugger.start(
      document.getElementById("myapp"),
      init,
      update,
      view,
      subscriptions,
      DebuggerOptions(
        msgToCompactString = _.showCompact,
        serialize = Some(
          DebuggerSerialize(
            encodeModel = _.asJsAny,
            decodeModel = jsAny => decodeJs[Model](jsAny),
            encodeMsg = _.asJsAny,
            decodeMsg = jsAny => decodeJs[Msg](jsAny)
          )
        ),
        sanitizeMsg = {
          case Msg.GifError(HttpError.DecodingFailure(_, response)) => Msg.GifError(HttpError.DecodingFailure("foo", response))
        },
        sanitizeModel = m => m.copy(topic = "bar")
      )
    )

  given Encoder[Response[String]] = deriveEncoder
  given Decoder[Response[String]] = deriveDecoder
  given Encoder[HttpError]        = deriveEncoder
  given Decoder[HttpError]        = deriveDecoder
  given Encoder[Msg]              = deriveEncoder
  given Decoder[Msg]              = deriveDecoder
  given Encoder[Model]            = deriveEncoder
  given Decoder[Model]            = deriveDecoder

  given ShowCompact[Status]                        = ShowCompact.derived
  given [A: ShowCompact]: ShowCompact[Response[A]] = ShowCompact.derived
  given ShowCompact[HttpError]                     = ShowCompact.derived
  given ShowCompact[Msg]                           = ShowCompact.derived

end Main

enum Msg:
  case MorePlease extends Msg
  case NewGif(result: String) extends Msg
  case GifError(error: HttpError) extends Msg
object Msg:
  def fromHttpResponse: Either[HttpError, String] => Msg =
    case Left(e)  => Msg.GifError(e)
    case Right(s) => Msg.NewGif(s)

final case class Model(topic: String, gifUrl: String)

object HttpHelper:
  private def decodeGifUrl: Http.Decoder[String] =
    Http.Decoder { response =>
      val json = response.body
      parse(json)
        .leftMap(_.message)
        .flatMap { json =>
          json.hcursor
            .downField("data")
            .downField("images")
            .downField("fixed_height")
            .get[String]("url")
            .toOption
            .toRight("wrong json format")
        }
    }

  def getRandomGif(topic: String): Cmd[Msg] =
    val url =
      s"https://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=$topic"
    Http.send(Request.get(url, decodeGifUrl), Msg.fromHttpResponse)
