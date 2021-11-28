package tyrian.debugger.runtime

import org.scalajs.dom.Element
import tyrian.Cmd
import tyrian.Html
import tyrian.Sub
import tyrian.debugger.DebuggerOptions
import tyrian.debugger.runtime.DebuggerRuntime._
import tyrian.runtime.TyrianRuntime

import scala.reflect.ClassTag
import scala.scalajs.js

final class DebuggerRuntime[Model: ClassTag, Msg: ClassTag](
    init: (Model, Cmd[Msg]),
    update: (Msg, Model) => (Model, Cmd[Msg]),
    view: Model => Html[Msg],
    subscriptions: Model => Sub[Msg],
    node: Element,
    devtoolsExtension: DevtoolsExtension[Model, MsgAction[Msg]],
    options: DebuggerOptions[Model, Msg]
)(using eqModel: CanEqual[Model, Model], eqMsg: CanEqual[Msg, Msg]) {
  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  var latestUpdate: (Option[(Msg, Model)], Model) = (None, init._1)

  val devtoolsStore = devtoolsExtension((reducer _), js.undefined, devtoolsOptions)

  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  def reducer(undefOrState: js.UndefOr[Model], action: MsgAction[Msg]): Model =
    (undefOrState.toOption, action) match {
      case (Some(model), MsgAction(_, Some(msg))) =>
        if (latestUpdate._1.exists { case (ms, md) => ms == msg && md == model }) latestUpdate._2
        else update(msg, model)._1
      case (model, MsgAction("@@INIT", None)) =>
        model.getOrElse(init._1)
      case (_, MsgAction(typ, _)) =>
        throw new RuntimeException(s"Unexpected action type '$typ' dispatched from Devtools")
    }

  def devtoolsOptions: DevtoolsOptions[Model, MsgAction[Msg]] = DevtoolsOptions(
    serialize = options.serialize
      .fold(js.undefined) { ser =>
        Serialize(
          replacer = (_, value) =>
            value match {
              case model: Model => MarkedSerialized("Model", ser.encodeModel(model))
              case msg: Msg     => MarkedSerialized("Msg", ser.encodeMsg(msg))
              case _            => value
            },
          reviver = (_, value) =>
            value match {
              case MarkedSerialized("Model", data) => ser.decodeModel(data).toTry.get
              case MarkedSerialized("Msg", data)   => ser.decodeMsg(data).toTry.get
              case _                               => value
            }
        )
      },
    features = Features(
      pause = false,
      lock = true,
      persist = false,
      `export` =
        if (options.serialize.isDefined) "custom".asInstanceOf[js.|[Boolean, String]]
        else false.asInstanceOf[js.|[Boolean, String]],
      `import` =
        if (options.serialize.isDefined) "custom".asInstanceOf[js.|[Boolean, String]]
        else false.asInstanceOf[js.|[Boolean, String]],
      jump = true,
      skip = true,
      reorder = true,
      dispatch = false,
      test = true
    ),
    actionSanitizer = (action: MsgAction[Msg]) =>
      action match
        case MsgAction(typ, Some(options.sanitizeMsg(msg))) => MsgAction(typ, msg)
        case action                                         => action
    ,
    stateSanitizer = (state: Model) => options.sanitizeModel.applyOrElse(state, identity),
    maxAge = options.msgBufferSize
  )

  def debuggerInit: (DebuggerModel[Model], Cmd[DebuggerMsg[Model, Msg]]) =
    val (model, debugeeCmd) = init
    (DebuggerModel(debugee = model, locked = false), debugeeCmd.map(DebuggerMsg.UpdateDebugee.apply))

  def debuggerUpdate(
      msg: DebuggerMsg[Model, Msg],
      model: DebuggerModel[Model]
  ): (DebuggerModel[Model], Cmd[DebuggerMsg[Model, Msg]]) =
    msg match
      case DebuggerMsg.SetModel(debugee) =>
        (model.copy(debugee = debugee), Cmd.SideEffect(() => latestUpdate = (None, debugee)))
      case DebuggerMsg.UpdateDebugee(_) if model.locked =>
        (model, Cmd.Empty)
      case DebuggerMsg.UpdateDebugee(debugeeMsg) =>
        val (debugee, debugeeCmd) = update(debugeeMsg, model.debugee)
        val notifyDevtools = Cmd.SideEffect { () =>
          latestUpdate = (Some(debugeeMsg -> model.debugee), debugee)
          val sanitizedMsg = options.sanitizeMsg.applyOrElse(debugeeMsg, identity)
          val `type` = options.msgToCompactString.applyOrElse(sanitizedMsg, _ => "Msg")
          devtoolsStore.dispatch(MsgAction(`type`, debugeeMsg))
        }
        (
          model.copy(debugee = debugee),
          notifyDevtools.combine(debugeeCmd.map(DebuggerMsg.UpdateDebugee.apply))
        )
      case DebuggerMsg.Lock =>
        (model.copy(locked = true), Cmd.Empty)
      case DebuggerMsg.Unlock =>
        (model.copy(locked = false), Cmd.Empty)

  def debuggerView(model: DebuggerModel[Model]): Html[DebuggerMsg[Model, Msg]] =
    view(model.debugee).map(DebuggerMsg.UpdateDebugee.apply)

  def debuggerSubscriptions(model: DebuggerModel[Model]): Sub[DebuggerMsg[Model, Msg]] =
    val debugeeSub = if (!model.locked) subscriptions(model.debugee) else Sub.Empty
    val devtoolsStoreSub = Sub.ofTotalObservable[DebuggerMsg[Model, Msg]](
      "__devtools_state_change",
      { observer =>
        @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
        var isLocked = false

        def onLock() = {
          isLocked = true
          observer.onNext(DebuggerMsg.Lock)
        }

        def onUnlock() = {
          isLocked = false
          observer.onNext(DebuggerMsg.Unlock)
        }

        if (devtoolsStore.liftedStore.getState().isLocked) {
          onLock()
        }

        val unsubscribe = devtoolsStore.subscribe { () =>
          val storeModel = devtoolsStore.getState()
          if (latestUpdate._2 != storeModel)
            observer.onNext(DebuggerMsg.SetModel(storeModel))

          (isLocked, devtoolsStore.liftedStore.getState().isLocked) match {
            case (true, false) =>
              onUnlock()
            case (false, true) =>
              onLock()
            case _ =>
              ()
          }
        }
        () => unsubscribe()
      }
    )
    devtoolsStoreSub.combine(debugeeSub.map(DebuggerMsg.UpdateDebugee.apply))

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
  final case class DebuggerModel[Model](debugee: Model, locked: Boolean)

  enum DebuggerMsg[+Model, +Msg] derives CanEqual:
    case SetModel(model: Model)
    case UpdateDebugee(msg: Msg)
    case Lock
    case Unlock

  sealed trait MsgAction[Msg] extends js.Object {
    val `type`: String
    val msg: js.UndefOr[Msg]
  }
  object MsgAction {
    def apply[Msg](`type`: String, msg: Msg): MsgAction[Msg] =
      js.Dynamic.literal(`type` = `type`, msg = msg.asInstanceOf[js.Any]).asInstanceOf[MsgAction[Msg]]

    def unapply[Msg](action: MsgAction[Msg]): Some[(String, Option[Msg])] =
      Some(action.`type`, action.msg.toOption)
  }
}
