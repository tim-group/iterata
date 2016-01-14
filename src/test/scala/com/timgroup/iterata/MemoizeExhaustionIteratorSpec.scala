package com.timgroup.iterata

import com.timgroup.iterata.MemoizeExhaustionIterator.Implicits._
import org.scalatest.{DiagrammedAssertions, FunSpec, Matchers}

class MemoizeExhaustionIteratorSpec extends FunSpec with Matchers with DiagrammedAssertions {

  describe("Demonstration of SI-9623: JoinIterator always calls hasNext on exhausted left iterator after `++`") {

    describe("after a single `++`") {

      it("JoinIterator always calls hasNext on exhausted left iterator") {
        val it1 = new IteratorWithExpensiveHasNext()
        val it2 = new IteratorWithExpensiveHasNext()

        (it1 ++ it2).foreach(_ => ())

        // Why it1.hasNext is called 21 times instead of the expected 1:
        //   - for each of the 10 values in it2, once in JoinIterator#hasNext,
        //     and again in JoinIterator#next()
        //   - plus the final time that JoinIterator#hasNext will return false
        it1.numTimesHasNextReturnedFalse should be(21)
      }

    }

    describe("after two `++` calls") {
      
      it("ConcatIterator never calls hasNext on exhausted left iterator") {
        val it1 = new IteratorWithExpensiveHasNext()
        val it2 = new IteratorWithExpensiveHasNext()

        (Iterator.empty ++ it1 ++ it2).foreach(_ => ())

        // Why it1.hasNext is called the expected 1 times:
        //   - the second `++` call creates a ConcatIterator instead of JoinIterator
        //   - ConcatIterator never calls hasNext again after it returns false
        it1.numTimesHasNextReturnedFalse should be(1)
      }

    }

  }

  describe("#memoizeExhaustion") {

    it("never calls underlying hasNext after first false") {
      val itA = new IteratorWithExpensiveHasNext()
      val itB = itA.memoizeExhaustion

      itB.foreach(_ => ())
      for (_ <- 1 to 20) { itB.hasNext }

      itA.numTimesHasNextReturnedFalse should be(1)
    }

    it("can be used to workaround the JoinIterator issue (SI-9623)") {
      val it1 = new IteratorWithExpensiveHasNext()
      val it2 = new IteratorWithExpensiveHasNext()

      (it1.memoizeExhaustion ++ it2).foreach(_ => ())

      // Why it1.hasNext is called the expected 1 time:
      //   - we memoize the first false value in the underlying iterator hasNext
      //   - subsequent calls to hasNext from JoinIterator get the memoized value
      it1.numTimesHasNextReturnedFalse should be(1)
    }
  }

  class IteratorWithExpensiveHasNext(values: Seq[Int] = (1 to 10).toList) extends Iterator[Int] {
    val it = values.iterator
    var numTimesHasNextReturnedFalse = 0

    override def hasNext: Boolean = {
      val r = it.hasNext
      if (!r)
        numTimesHasNextReturnedFalse += 1
      r
    }

    override def next(): Int = it.next()
  }

}
