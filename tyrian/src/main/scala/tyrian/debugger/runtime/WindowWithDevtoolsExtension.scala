package tyrian.debugger.runtime

import org.scalajs.dom.Window

import scala.scalajs.js

@js.native
trait Store[State, Action] extends js.Object {
  def dispatch(action: Action): Unit                              = js.native
  def subscribe(listener: js.Function0[Unit]): js.Function0[Unit] = js.native
  def getState(): State                                           = js.native
}

@js.native
trait DevtoolsStore[State, Action] extends js.Object {
  def dispatch(action: Action): Unit                              = js.native
  def subscribe(listener: js.Function0[Unit]): js.Function0[Unit] = js.native
  def getState(): State                                           = js.native
  def liftedStore: Store[LiftedState, Nothing]                    = js.native
}

@js.native
trait LiftedState extends js.Object {
  def isLocked: Boolean = js.native
}

@js.native
trait DevtoolsOptions[State, Action] extends js.Object {
  def serialize: js.UndefOr[Serialize]
  def features: js.UndefOr[Features]
  def actionSanitizer: js.UndefOr[js.Function1[Action, Action]]
  def stateSanitizer: js.UndefOr[js.Function1[State, State]]
  def maxAge: js.UndefOr[Int]
}
object DevtoolsOptions {
  def apply[State, Action](
      serialize: js.UndefOr[Serialize],
      features: js.UndefOr[Features],
      actionSanitizer: js.UndefOr[js.Function1[Action, Action]],
      stateSanitizer: js.UndefOr[js.Function1[State, State]],
      maxAge: js.UndefOr[Int]
  ): DevtoolsOptions[State, Action] =
    js.Dynamic
      .literal(
        serialize = serialize.asInstanceOf[js.Any],
        features = features.asInstanceOf[js.Any],
        actionSanitizer = actionSanitizer.asInstanceOf[js.Any],
        stateSanitizer = stateSanitizer.asInstanceOf[js.Any],
        maxAge = maxAge.asInstanceOf[js.Any]
      )
      .asInstanceOf[DevtoolsOptions[State, Action]]
}

@js.native
trait Serialize extends js.Object {
  def replacer: js.UndefOr[js.Function2[js.Any, js.Any, js.Any]]
  def reviver: js.UndefOr[js.Function2[js.Any, js.Any, Any]]
}
object Serialize {
  def apply[State, Action](
      replacer: js.UndefOr[js.Function2[js.Any, js.Any, js.Any]],
      reviver: js.UndefOr[js.Function2[js.Any, js.Any, Any]]
  ): Serialize =
    js.Dynamic
      .literal(replacer = replacer.asInstanceOf[js.Any], reviver = reviver.asInstanceOf[js.Any])
      .asInstanceOf[Serialize]
}

@js.native
trait Features extends js.Object {
  def pause: js.UndefOr[Boolean]
  def lock: js.UndefOr[Boolean]
  def persist: js.UndefOr[Boolean]
  def `export`: js.UndefOr[js.|[Boolean, String /* "custom" */ ]]
  def `import`: js.UndefOr[js.|[Boolean, String /* "custom" */ ]]
  def jump: js.UndefOr[Boolean]
  def skip: js.UndefOr[Boolean]
  def reorder: js.UndefOr[Boolean]
  def dispatch: js.UndefOr[Boolean]
  def test: js.UndefOr[Boolean]
}
object Features {
  def apply[State, Action](
      pause: js.UndefOr[Boolean],
      lock: js.UndefOr[Boolean],
      persist: js.UndefOr[Boolean],
      `export`: js.UndefOr[js.|[Boolean, String /* "custom" */ ]],
      `import`: js.UndefOr[js.|[Boolean, String /* "custom" */ ]],
      jump: js.UndefOr[Boolean],
      skip: js.UndefOr[Boolean],
      reorder: js.UndefOr[Boolean],
      dispatch: js.UndefOr[Boolean],
      test: js.UndefOr[Boolean]
  ): Features =
    js.Dynamic
      .literal(
        pause = pause.asInstanceOf[js.Any],
        lock = lock.asInstanceOf[js.Any],
        persist = persist.asInstanceOf[js.Any],
        `export` = `export`.asInstanceOf[js.Any],
        `import` = `import`.asInstanceOf[js.Any],
        jump = jump.asInstanceOf[js.Any],
        skip = skip.asInstanceOf[js.Any],
        reorder = reorder.asInstanceOf[js.Any],
        dispatch = dispatch.asInstanceOf[js.Any],
        test = test.asInstanceOf[js.Any]
      )
      .asInstanceOf[Features]
}

@js.native
trait MarkedSerialized extends js.Object {
  def `__serializedType__` : String
  def data: js.Any
}
object MarkedSerialized {
  def apply(`__serializedType__`: String, data: js.Any): MarkedSerialized =
    js.Dynamic.literal(`__serializedType__` = `__serializedType__`, data = data).asInstanceOf[MarkedSerialized]

  @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
  def unapply(jsValue: js.Any): Option[(String, js.Any)] =
    jsValue match {
      case jsObject: js.Object if jsObject != null && jsObject.hasOwnProperty("__serializedType__") =>
        val dyn = jsObject.asInstanceOf[js.Dynamic]
        Some((dyn.`__serializedType__`.asInstanceOf[String]), dyn.data)
      case _ =>
        None
    }
}

type DevtoolsExtension[State, Action <: js.Object] =
  js.Function3[js.Function2[js.UndefOr[State], Action, State], js.UndefOr[State], js.UndefOr[DevtoolsOptions[
    State,
    Action
  ]], DevtoolsStore[State, Action]]

@js.native
trait WindowWithDevtoolsExtension extends Window {
  def `__REDUX_DEVTOOLS_EXTENSION__`[State, Action <: js.Object]: js.UndefOr[DevtoolsExtension[State, Action]] =
    js.native
}

object WindowWithDevtoolsExtension {
  implicit def windowToWindowWithDevtoolsExtension(window: Window): WindowWithDevtoolsExtension =
    window.asInstanceOf[WindowWithDevtoolsExtension]
}
