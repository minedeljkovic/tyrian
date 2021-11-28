package tyrian.debugger

import scala.scalajs.js

final case class DebuggerOptions[Model, Msg](
    msgToCompactString: PartialFunction[Msg, String] = PartialFunction.empty,
    serialize: Option[DebuggerSerialize[Model, Msg]] = None,
    sanitizeMsg: PartialFunction[Msg, Msg] = PartialFunction.empty,
    sanitizeModel: PartialFunction[Model, Model] = PartialFunction.empty,
    msgBufferSize: Int = 50
)

final case class DebuggerSerialize[Model, Msg](
    encodeModel: Model => js.Any,
    decodeModel: js.Any => Either[Throwable, Model],
    encodeMsg: Msg => js.Any,
    decodeMsg: js.Any => Either[Throwable, Msg]
)
