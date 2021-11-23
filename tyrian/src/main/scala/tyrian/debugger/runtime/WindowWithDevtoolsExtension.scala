package tyrian.debugger.runtime

import org.scalajs.dom.Window

import scala.scalajs.js

@js.native
trait DevtoolsStore[State, Action] extends js.Object {
  def dispatch(action: Action): Unit                              = js.native
  def subscribe(listener: js.Function0[Unit]): js.Function0[Unit] = js.native
  def getState(): State                                           = js.native
}

type DevtoolsExtension[State, Action <: js.Object] =
  js.Function1[js.Function2[js.UndefOr[State], Action, State], DevtoolsStore[State, Action]]

@js.native
trait WindowWithDevtoolsExtension extends Window {
  def `__REDUX_DEVTOOLS_EXTENSION__`[State, Action <: js.Object]: js.UndefOr[DevtoolsExtension[State, Action]] =
    js.native
}

object WindowWithDevtoolsExtension {
  implicit def windowToWindowWithDevtoolsExtension(window: Window): WindowWithDevtoolsExtension =
    window.asInstanceOf[WindowWithDevtoolsExtension]
}
