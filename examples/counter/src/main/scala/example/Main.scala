package example

import org.scalajs.dom.document
import tyrian.Html
import tyrian.Html._
import tyrian.debugger.Debugger
import tyrian.debugger.DebuggerOptions

object Main:
  opaque type Model = Int

  def main(args: Array[String]): Unit =
    Debugger.start(document.getElementById("myapp"), init, update, view, DebuggerOptions(msgToCompactString = _.showCompact))

  def init: Model = 0

  def update(msg: Msg, model: Model): Model =
    msg match
      case Msg.Increment => model + 1
      case Msg.Decrement => model - 1

  def view(model: Model): Html[Msg] =
    div()(
      button(onClick(Msg.Decrement))(text("-")),
      div()(text(model.toString)),
      button(onClick(Msg.Increment))(text("+"))
    )

enum Msg:
  case Increment, Decrement
  
  def showCompact: String =
    this match
      case Increment => "Increment"
      case Decrement => "Decrement"
end Msg