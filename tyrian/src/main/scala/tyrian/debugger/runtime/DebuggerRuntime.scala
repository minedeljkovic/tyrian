package tyrian.debugger.runtime

import org.scalajs.dom.Element
import tyrian.Cmd
import tyrian.Html
import tyrian.Sub
import tyrian.debugger.runtime.DebuggerRuntime._
import tyrian.runtime.TyrianRuntime

import scala.scalajs.js

final class DebuggerRuntime[Model, Msg](
    init: (Model, Cmd[Msg]),
    update: (Msg, Model) => (Model, Cmd[Msg]),
    view: Model => Html[Msg],
    subscriptions: Model => Sub[Msg],
    node: Element,
    devtoolsExtension: DevtoolsExtension[Model, MsgAction[Msg]]
)(using eqModel: CanEqual[Model, Model], eqMsg: CanEqual[Msg, Msg]) {
  val memoizedUpdate    = update.memoized
  val updateReadsCache  = memoizedUpdate.onlyReadsCache
  val updateWritesCache = memoizedUpdate.onlyWritesCache

  val devtoolsStore = devtoolsExtension((reducer _))

  def reducer(undefOrState: js.UndefOr[Model], action: MsgAction[Msg]): Model =
    (undefOrState.toOption, action) match {
      case (Some(model), action @ MsgAction(msg)) if action.`type` != "@@INIT" =>
        updateReadsCache(msg, model)._1
      case (model, _) =>
        model.getOrElse(init._1)
    }

  def debuggerInit: (Model, Cmd[DebuggerMsg[Model, Msg]]) =
    val (model, debugeeCmd) = init
    (model, debugeeCmd.map(DebuggerMsg.UpdateDebugee.apply))

  def debuggerUpdate(msg: DebuggerMsg[Model, Msg], model: Model): (Model, Cmd[DebuggerMsg[Model, Msg]]) =
    msg match
      case DebuggerMsg.SetModel(model) =>
        (model, Cmd.Empty)
      case DebuggerMsg.UpdateDebugee(debugeeMsg) =>
        val (newModel, debugeeCmd) = updateWritesCache(debugeeMsg, model)
        (
          newModel,
          devtoolsStore.dispatchCmd(MsgAction(debugeeMsg)).combine(debugeeCmd.map(DebuggerMsg.UpdateDebugee.apply))
        )

  def debuggerView(model: Model): Html[DebuggerMsg[Model, Msg]] =
    view(model).map(DebuggerMsg.UpdateDebugee.apply)

  def debuggerSubscriptions(model: Model): Sub[DebuggerMsg[Model, Msg]] =
    val debugeeSub = subscriptions(model)
    devtoolsStore
      .modelChangeSub(model)
      .map(DebuggerMsg.SetModel.apply)
      .combine(debugeeSub.map(DebuggerMsg.UpdateDebugee.apply))

  def start(): Unit =
    new TyrianRuntime(
      debuggerInit,
      debuggerUpdate,
      debuggerView,
      debuggerSubscriptions,
      node
    ).start()
}

object DebuggerRuntime {
  enum DebuggerMsg[Model, Msg]:
    case SetModel(model: Model)
    case UpdateDebugee(msg: Msg)

  trait MsgAction[Msg] extends js.Object {
    val `type`: String
    val value: Msg
  }
  object MsgAction {
    def apply[Msg](msg: Msg): MsgAction[Msg] =
      js.Dynamic.literal(`type` = "Msg", value = msg.asInstanceOf[js.Any]).asInstanceOf[MsgAction[Msg]]

    def unapply[Msg](action: MsgAction[Msg]): Option[Msg] = Some(action.value)
  }

  extension [Model, Msg](store: DevtoolsStore[Model, Msg])
    def dispatchCmd(msg: Msg): Cmd[Nothing] =
      Cmd.SideEffect(() => store.dispatch(msg))

    def modelChangeSub(model: Model)(using eqModel: CanEqual[Model, Model]): Sub[Model] =
      Sub.ofTotalObservable[Model](
        "__devtools_state_change",
        { observer =>
          val unsubscribe = store.subscribe(() => observer.onNext(store.getState()))
          () => unsubscribe()
        }
      )
}
