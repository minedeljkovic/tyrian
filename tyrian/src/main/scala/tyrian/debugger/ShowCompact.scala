package tyrian.debugger

import scala.compiletime.erasedValue
import scala.compiletime.summonInline
import scala.deriving.*

inline def summonAll[T <: Tuple]: List[ShowCompact[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonInline[ShowCompact[t]] :: summonAll[ts]

trait ShowCompact[A]:
  extension(a: A) def showCompact: String

object ShowCompact:
  given ShowCompact[Boolean] with
    extension(a: Boolean) def showCompact: String = if a then "True" else "False"

  given numericCompactShow[A: Numeric]: ShowCompact[A] with
    extension(a: A) def showCompact: String = a.toString

  given ShowCompact[String] with
    extension(a: String) def showCompact: String = "\"" + a + "\""

  given ShowCompact[Char] with
    extension(a: Char) def showCompact: String = "'" + a + "'"

  given iterable1CompactShow[A, F[_] <: Iterable[Any]]: ShowCompact[F[A]] with
    extension(a: F[A]) def showCompact: String = "..."

  given iterable2CompactShow[A, B, F[_, _] <: Iterable[Any]]: ShowCompact[F[A, B]] with
    extension(a: F[A, B]) def showCompact: String = "..."

  def showCompactSum[A](s: Mirror.SumOf[A], elems: => List[ShowCompact[_]]): ShowCompact[A] =
    new ShowCompact[A]:
      extension(a: A) def showCompact: String =
        val ordx = s.ordinal(a)
        elems(ordx).asInstanceOf[ShowCompact[A]].showCompact(a)

  def showCompactProduct[A](label: String, elems: => List[ShowCompact[_]]): ShowCompact[A] =
    new ShowCompact[A]:
      extension(a: A) def showCompact: String =
        val showElems = a.asInstanceOf[Product].productIterator.zip(elems.iterator).map {
          case (x, elem) => elem.asInstanceOf[ShowCompact[Any]].showCompact(x)
        }
        showElems.mkString(s"$label ", " ", "")

  inline given derived[A](using m: Mirror.Of[A]): ShowCompact[A] =
    lazy val elemInstances = summonAll[m.MirroredElemTypes]
    lazy val label = summonInline[ValueOf[m.MirroredLabel]].value
    inline m match
      case s: Mirror.SumOf[A]     => showCompactSum(s, elemInstances)
      case p: Mirror.ProductOf[A] => showCompactProduct(label, elemInstances)
    
end ShowCompact
