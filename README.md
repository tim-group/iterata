[![Build Status](https://travis-ci.org/tim-group/iterata.svg)](https://travis-ci.org/tim-group/iterata)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.timgroup/iterata_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.timgroup/iterata_2.11)

# iterata
Useful extensions to Scala's Iterator. Think _errata for iterators_.

## Installation

Using SBT:

```sbt
libraryDependencies += "com.timgroup" %% "iterata" % "0.1.3"
```

Or download the jar [directly from maven central](https://repo1.maven.org/maven2/com/timgroup/iterata_2.11/).

Iterata is currently published for Scala 2.11 only, please feel free to let us know if you'd like a build for a different Scala version.

## Usage

### #par()

Use the `#par()` method to add parallelism when processing an `Iterator` with functions chained via `#map` and `#flatMap`. It will eagerly evaluate the underlying iterator in chunks, and then evaluate the functions on each chunk via the Scala Parallel Collections. For example:

```scala
scala> import com.timgroup.iterata.ParIterator.Implicits._
scala> val it = (1 to 100000).toIterator.par().map(n => (n + 1, Thread.currentThread.getId))
scala> it.map(_._2).toSet.size
res2: Int = 8 // addition was distributed over 8 threads
```

You can provide a specific chunk size, for example `it.par(100)`.

#### Grouped vs Ungrouped

The `#par()` method is available on any iterator, and takes an optional chunk size parameter. However, if you already have a `GroupedIterator`, you can simply call `#par` since it is already grouped. For example:

```scala
scala> val it = (1 to 100000).toIterator.grouped(4).par
```
