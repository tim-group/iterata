package com.timgroup.iterata

import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicInteger

import com.timgroup.iterata.ParIterator.Implicits._
import org.scalatest.{DiagrammedAssertions, FunSpec, Matchers, Tag}

import scala.collection.parallel.{Task, TaskSupport}

object Performance extends Tag("Performance")

class ParIteratorSpec extends FunSpec with Matchers with DiagrammedAssertions {

  describe("performance") {

    it("Faster #map on fast iterator with slow function", Performance) {
      def s = Stream.continually(1).take(4000)   // how much can we speed up processing each element for 1 ms?
      val l = s.toList                           // baseline: parallelize a non-lazy list
      def it = s.iterator                        // comparison: parallelize a lazy iterator in chunks
      def f(n: Int) = { Thread.sleep(1); n + 1 } // a function to parallelize, taking 1 ms for each element

      val millisNoPar = bm { l.map(f).foreach(_ => ()) }.toDouble
      println(s"Duration 1, no parallel:\t$millisNoPar")

      val millisWithParIt = bm { it.par(1000).map(f).foreach(_ => ()) }.toDouble
      println(s"Duration 2, it.par(1000):\t$millisWithParIt")

      val millisWithParLst = bm { l.par.map(f).foreach(_ => ()) }.toDouble
      println(s"Duration 3, l.par:\t\t$millisWithParLst")

      millisWithParIt shouldBe (millisWithParLst +- (0.5 * math.max(millisWithParIt, millisWithParLst)))
    }

    it("but, same speed #map on slow iterator with fast function", Performance) {
      def it = Stream.continually({ Thread.sleep(1); 1 }).take(4000).iterator
      def f(n: Int) = n + 1

      val millisNoPar = bm { it.map(f).foreach(_ => ()) }
      println(s"Duration 4, no parallel:\t$millisNoPar")

      val millisWithPar = bm { it.par(1000).map(f).foreach(_ => ()) }
      println(s"Duration 5, par(1000):\t\t$millisWithPar")

      millisNoPar shouldBe (millisWithPar +- (millisNoPar / 10))
    }

    it("Faster through #map, #flatMap, #map, #flatMap on fast iterator with slow function", Performance) {
      def extend(it: Iterator[Int]) = it
        .map(identity)
        .flatMap(n => List(n))
        .map(identity)
        .flatMap(n => List(n))

      def f(n: Int) = { Thread.sleep(1); n + 1 } // a function to parallelize, taking 1 ms for each element
      def s = Stream.continually(1).take(4000)   // how much can we speed up processing each element for 1 ms?
      def it1 = s.iterator                       // baseline: parallelize a lazy iterator in chunks
      def it2 = it1                              // comparison: transform iterator a few times before parallelism

      val millisWithParIt1 = bm { it1.par(1000).map(f).foreach(_ => ()) }.toDouble
      println(s"Duration 6, it1.par(1000):\t$millisWithParIt1")

      val millisWithParIt2 = bm { extend(it1.par(1000)).map(f).foreach(_ => ()) }.toDouble
      println(s"Duration 7, it2.par(1000):\t$millisWithParIt2")

      millisWithParIt2 shouldBe (millisWithParIt1 +- (0.5 * millisWithParIt1))
    }

    it("Faster #find on fast iterator with slow predicate", Performance) {
      def s = Stream.continually(1).take(4000)   // how much can we speed up evaluating a predicate on each element for 1 ms?
      val l = s.toList                           // baseline: parallelize a non-lazy list
      def it = s.iterator                        // comparison: parallelize a lazy iterator in chunks
      def f(n: Int) = { Thread.sleep(1); false } // a predicate to parallelize, taking 1 ms for each element

      val millisNoPar = bm { l.find(f) }.toDouble
      println(s"Duration 8, no parallel:\t$millisNoPar")

      val millisWithParIt = bm { it.par(1000).find(f) }.toDouble
      println(s"Duration 9, it.par(1000):\t$millisWithParIt")

      val millisWithParLst = bm { l.par.find(f) }.toDouble
      println(s"Duration 10, l.par:\t\t$millisWithParLst")

      millisWithParIt shouldBe (millisWithParLst +- (0.5 * millisWithParLst))
    }

    def bm(f: => Unit): Long = {
      val t0 = System.currentTimeMillis
      f
      System.currentTimeMillis - t0
    }

  }

  describe("correctness") {

    describe("#flatMap") {
      it("applies function to each element across chunks") {
        val it = (1 to 10).iterator.par(3)
        val xs = it.flatMap(n => List(n - 1))
        xs.toList shouldBe (0 to 9).toList
      }

      it("when iterator partially advanced into chunk") {
        val it = (1 to 10).toList.iterator.par(3)
        for (_ <- 1 to 5) it.next()
        val xs = it.flatMap(n => List(n - 1))
        xs.toList shouldBe (5 to 9).toList
      }

      it("passes on exception thrown by underlying iterator #next") {
        val ex = new RuntimeException("uh oh")
        val it = List(1).iterator.map { n => throw ex; n }.par(3)
        intercept[RuntimeException] { it.flatMap(n => List(n)).toList } shouldBe ex
      }

      it("preserves laziness in subsequent chunks when partially advanced into first chunk") {
        var effectOccurred = false
        val it = (1 to 10).iterator.map { n => effectOccurred ||= (n == 4); n }.par(3)
        it.next()
        val xs = it.flatMap(n => List(n))
        effectOccurred shouldBe false
      }

      it("extends parallel execution to function in a second #flatMap") {
        val it1 = (1 to 10000).iterator.par(1000)
        val it2 = it1.flatMap(n => Seq((n, Thread.currentThread.getId)))
        val it3 = it2.flatMap { case (n, id) => Seq((n, id, Thread.currentThread.getId)) }
        //println(it3.toList)
        val threadIdsInSecondFlatMap = it3.toList.map(_._3).toSet
        threadIdsInSecondFlatMap.size shouldBe > (1)
      }

      it("handles when underlying iterator is already exhausted") {
        val it = Seq(1).iterator.par(1)
        it.next()
        it.flatMap(n => Seq(n)).toList shouldBe Nil
      }

      it("handles when function applied leaves no elements in current chunk") {
        val it = Seq(1, 2).iterator.par(2)
        it.next() // current chunk should still contain 2
        it.flatMap(n => Nil).toList shouldBe Nil
      }
    }

    describe("#map") {
      it("applies function to each element across chunks") {
        val it = (1 to 10).toList.iterator.par(3)
        val xs = it.map(n => n - 1)
        xs.toList shouldBe (0 to 9).toList
      }

      it("when iterator partially advanced into chunk") {
        val it = (1 to 10).toList.iterator.par(3)
        for (_ <- 1 to 5) it.next()
        val xs = it.map(n => n - 1)
        xs.toList shouldBe (5 to 9).toList
      }

      it("passes on exception thrown by underlying iterator #next") {
        val ex = new RuntimeException("uh oh")
        val it = List(1).iterator.map { n => throw ex; n }.par(3)
        intercept[RuntimeException] { it.map(n => n).toList } shouldBe ex
      }

      it("preserves laziness in subsequent chunks when partially advanced into first chunk") {
        var effectOccurred = false
        val it = (1 to 10).iterator.map { n => effectOccurred ||= (n == 4); n }.par(3)
        it.next()
        val xs = it.map(n => n)
        effectOccurred shouldBe false
      }

      it("extends parallel execution to function in a second #map") {
        val it1 = (1 to 10000).iterator.par(1000)
        val it2 = it1.map(n => (n, Thread.currentThread.getId))
        val it3 = it2.map { case (n, id) => (n, id, Thread.currentThread.getId) }
        //println(it3.toList)
        val threadIdsInSecondMap = it3.toList.map(_._3).toSet
        threadIdsInSecondMap.size shouldBe > (1)
      }

      it("handles when underlying iterator is already exhausted") {
        val it = Seq(1).iterator.par(1)
        it.next()
        it.map(identity).toList shouldBe Nil
      }
    }

    describe("#filter") {
      it("filters by applying predicate to each element across chunks") {
        val it = (1 to 10).toList.iterator.par(3)
        val xs = it.filter(n => n % 3 != 0)
        xs.toList shouldBe List(1, 2, 4, 5, 7, 8, 10)
      }

      it("when iterator partially advanced into chunk") {
        val it = (1 to 10).toList.iterator.par(3)
        for (_ <- 1 to 5) it.next()
        val xs = it.filter(n => n % 3 != 0)
        xs.toList shouldBe List(7, 8, 10)
      }

      it("passes on exception thrown by underlying iterator #next") {
        val ex = new RuntimeException("uh oh")
        val it = List(1).iterator.map { n => throw ex; n }.par(3)
        intercept[RuntimeException] { it.filter(n => n % 3 != 0).toList } shouldBe ex
      }

      it("preserves laziness in subsequent chunks when partially advanced into first chunk") {
        var effectOccurred = false
        val it = (1 to 10).iterator.map { n => effectOccurred ||= (n == 4); n }.par(3)
        it.next()
        val xs = it.filter(n => n % 3 != 0)
        effectOccurred shouldBe false
      }

      it("extends parallel execution to function in a second #filter") {
        var threadIds1 = Set[Long]()
        var threadIds2 = Set[Long]()

        val it1 = (1 to 10000).iterator.par(1000)
        val it2 = it1.filter { n => synchronized { threadIds1 += Thread.currentThread.getId }; true }
        val it3 = it2.filter { n => synchronized { threadIds2 += Thread.currentThread.getId }; true }
        it3.foreach(_ => ())
        threadIds1.size shouldBe > (1)
        threadIds2.size shouldBe > (1)
      }

      it("handles when underlying iterator is already exhausted") {
        val it = Seq(1).iterator.par(1)
        it.next()
        it.filter(n => true).toList shouldBe Nil
      }

      it("handles when filter predicate leaves no elements in current chunk") {
        val it = Seq(1, 2).iterator.par(2)
        it.next() // current chunk should still contain 2
        it.filter(_ => false).toList shouldBe Nil
      }
    }

    describe("#find") {
      it("finds by applying predicate to each element across chunks until first true") {
        val it = (1 to 10).toList.iterator.par(3)
        val maybeN = it.find(n => n > 3)
        maybeN should be(Some(4))
      }

      it("when iterator partially advanced into chunk") {
        val it = (1 to 10).toList.iterator.par(3)
        for (_ <- 1 to 5) it.next()
        val maybeN = it.find(n => n > 3)
        maybeN should be(Some(6))
      }

      it("returns None when predicate never true") {
        val it = (1 to 10).toList.iterator.par(3)
        val maybeN = it.find(n => false)
        maybeN should be(None)
      }

      it("passes on exception thrown by underlying iterator #next") {
        val ex = new RuntimeException("uh oh")
        val it = List(1).iterator.map { n => throw ex; n }.par(3)
        intercept[RuntimeException] { it.find(n => n > 3).toList } shouldBe ex
      }

      it("triggers effect when found in same chunk") {
        var effectOccurred = false
        val it = (1 to 10).iterator.map { n => effectOccurred ||= (n == 5); n }.par(3)
        it.next()
        val maybeN = it.find(n => n == 4)
        effectOccurred shouldBe true
      }

      it("doesn't trigger effect when found in earlier chunk") {
        var effectOccurred = false
        val it = (1 to 10).iterator.map { n => effectOccurred ||= (n == 5); n }.par(3)
        it.next()
        val maybeN = it.find(n => n == 2)
        effectOccurred shouldBe false
      }

      it("finds in parallel across threads") {
        var threadIds = Set[Long]()

        val it = (1 to 10000).iterator.par(1000)
        val maybeN = it.find { n => synchronized { threadIds += Thread.currentThread.getId }; false }

        threadIds.size shouldBe > (1)
      }
    }

    describe("#hasNext") {
      it("returns false when underlying iterator was already empty") {
        List[Int]().iterator.par(3).hasNext shouldBe false
      }

      it("returns false when all elements have been returned") {
        val it = List(1).iterator.par(1)
        it.next()
        it.hasNext shouldBe false
      }

      it("returns true when elements remain in first chunk") {
        val it = List(1).iterator.par(1)
        it.hasNext shouldBe true
      }

      it("returns true when elements remain after first chunk") {
        val it = List(1, 2, 3).iterator.par(2)
        for (_ <- 1 to 2) it.next()
        it.hasNext shouldBe true
      }
    }

    describe("#next") {
      it("returns each element across chunks") {
        val it = (1 to 10).toList.iterator.par(3)
        val xs = for (_ <- 1 to 10) yield it.next()
        xs.toList shouldBe (1 to 10).toList
      }

      it("throws when no elements remaining") {
        intercept[NoSuchElementException] {
          List[Int]().iterator.par(3).next()
        }
      }

      it("passes on exception thrown by underlying iterator #next") {
        val ex = new RuntimeException("uh oh")
        val it = List(1).iterator.map { n => throw ex; n }.par(3)
        intercept[RuntimeException] { it.next() } shouldBe ex
      }
    }

  }

  class CountingTaskSupport(underlying: TaskSupport = scala.collection.parallel.defaultTaskSupport) extends TaskSupport {
    def count = _count.get()
    private val _count = new AtomicInteger(0)
    override val environment: AnyRef = underlying.environment
    override def parallelismLevel: Int = underlying.parallelismLevel
    override def execute[R, Tp](fjtask: Task[R, Tp]): () => R = {
      _count.incrementAndGet()
      underlying.execute(fjtask)
    }

    override def executeAndWaitResult[R, Tp](task: Task[R, Tp]): R = {
      _count.incrementAndGet()
      underlying.executeAndWaitResult(task)
    }
  }

  describe("taskSupport") {
    it("is used in #filter") {
      val ts = new CountingTaskSupport()
      val it = (1 to 10).iterator.par(3, ts)
      val xs = it.filter(n => n % 3 != 0).toList
      xs should have size 7
      ts.count shouldBe 8
    }

    it("is used in #find") {
      val ts = new CountingTaskSupport()
      val it = (1 to 10).toList.iterator.par(3, ts)
      val maybeN = it.find(n => n > 3)
      maybeN should be(Some(4))
      ts.count shouldBe 2
    }

    it("is used in #flatMap") {
      val ts = new CountingTaskSupport()
      val it = (1 to 10).iterator.par(3, ts)
      val xs = it.flatMap(n => List(n - 1)).toList
      xs should have size 10
      ts.count shouldBe 8
    }

    it("is used in #map") {
      val ts = new CountingTaskSupport()
      val it = (1 to 10).toList.iterator.par(3, ts)
      val xs = it.map(n => n - 1).toList
      xs should have size 10
      ts.count shouldBe 8
    }
  }

}
