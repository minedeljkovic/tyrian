package tyrian.debugger

import org.scalajs.dom.Element
import org.scalajs.dom.window
import tyrian.Cmd
import tyrian.Html
import tyrian.Sub
import tyrian.debugger.runtime.DebuggerRuntime
import tyrian.debugger.runtime.DebuggerRuntime.MsgAction
import tyrian.debugger.runtime.WindowWithDevtoolsExtension._
import tyrian.runtime.TyrianRuntime

object Debugger:

  given CanEqual[Option[_], Option[_]] = CanEqual.derived

  def start[Model, Msg](
      node: Element,
      init: Model,
      update: (Msg, Model) => Model,
      view: Model => Html[Msg]
  )(using eqModel: CanEqual[Model, Model], eqMsg: CanEqual[Msg, Msg]): Unit =
    start(
      node,
      (init, Cmd.Empty),
      (msg: Msg, m: Model) => (update(msg, m), Cmd.Empty),
      view,
      _ => Sub.Empty
    )

  def start[Model, Msg](
      node: Element,
      init: (Model, Cmd[Msg]),
      update: (Msg, Model) => (Model, Cmd[Msg]),
      view: Model => Html[Msg],
      subscriptions: Model => Sub[Msg]
  )(using eqModel: CanEqual[Model, Model], eqMsg: CanEqual[Msg, Msg]): Unit =
    window.__REDUX_DEVTOOLS_EXTENSION__[Model, MsgAction[Msg]].toOption match {
      case Some(devtoolsExtension) =>
        new DebuggerRuntime(
          init,
          update,
          view,
          subscriptions,
          node,
          devtoolsExtension
        ).start()
      case None =>
        new TyrianRuntime(
          init,
          update,
          view,
          subscriptions,
          node
        ).start()
    }
