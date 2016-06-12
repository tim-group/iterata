package com.timgroup.iterata

/** An iterator which memoizes the first call to `hasNext` which returns false.
  *
  * This is useful when the wrapped iterator needs to do I/O in hasNext, preventing
  * further expensive calls. It is also a suitable workaround for SI-9623.
  *
  * See: https://issues.scala-lang.org/browse/SI-9623
  *
  * {{{
  *   scala> import com.timgroup.iterata.MemoizeExhaustionIterator.Implicits._
  *   scala> val it1 = new IteratorWithExpensiveHasNext()
  *   scala> val it2 = new IteratorWithExpensiveHasNext()
  *   scala> (it1.memoizeExhaustion ++ it2).foreach(_ => ())
  *   scala> it1.numTimesHasNextReturnedFalse
  *   res2: Int = 1
  * }}}
  *
  * @param it    an underlying iterator for which to memoize the first false result from hasNext
  * @tparam A    the type of each element
  */
class MemoizeExhaustionIterator[A](it: Iterator[A]) extends Iterator[A] {
  var shouldForwardHasNext = true

  override def hasNext: Boolean = {
    if (shouldForwardHasNext) { shouldForwardHasNext = it.hasNext }
    shouldForwardHasNext
  }

  override def next(): A = it.next()
}

object MemoizeExhaustionIterator {

  object Implicits {

    implicit class IteratorWithMemoizeExhaustion[A](it: Iterator[A]) {
      def memoizeExhaustion: Iterator[A] =
        new MemoizeExhaustionIterator[A](it)
    }

  }

}