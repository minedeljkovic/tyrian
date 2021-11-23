package tyrian.debugger.runtime

@SuppressWarnings(Array("scalafix:DisableSyntax.var"))
final case class Cache[A](var value: Option[A])

/** Massively simplifed memoization implementation, which doesn't care about concurrency, since this is intended to use
  * only in single threaded scalajs environment
  */
final class MemoizedFunction2[T1, T2, R](
    f: Function2[T1, T2, R],
    cache: Cache[(T1, T2, R)],
    readsCache: Boolean = true,
    writesCache: Boolean = true
)(using eq1: CanEqual[T1, T1], eq2: CanEqual[T2, T2]) {
  def apply(v1: T1, v2: T2): R =
    (readsCache, cache.value) match {
      case (true, Some(vc1, vc2, rc)) if v1 == vc1 && v2 == vc2 => rc
      case _ =>
        val r = f(v1, v2)
        if (writesCache) cache.value = Some((v1, v2, r))
        r
    }

  def onlyReadsCache: MemoizedFunction2[T1, T2, R] =
    new MemoizedFunction2(f, cache, readsCache = true, writesCache = false)

  def onlyWritesCache: MemoizedFunction2[T1, T2, R] =
    new MemoizedFunction2(f, cache, readsCache = false, writesCache = true)
}

extension [T1, T2, R](function2: Function2[T1, T2, R])(using eq1: CanEqual[T1, T1], eq2: CanEqual[T2, T2])
  def memoized: MemoizedFunction2[T1, T2, R] = new MemoizedFunction2(function2, Cache(None))
