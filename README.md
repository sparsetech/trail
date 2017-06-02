# Trail
[![Build Status](https://travis-ci.org/sparsetech/trail.svg)](https://travis-ci.org/sparsetech/trail)
[![Join the chat at https://gitter.im/sparsetech/trail](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sparsetech/trail?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/tech.sparse/trail.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22sparse.tech%22)

Trail is a routing library for Scala and Scala.js.

## Example
```scala
import trail._
import shapeless._

val details  = Root / "details" / Arg[Int]
val userInfo = Root / "user" / Arg[String] & Param[Boolean]("show")

val result = "/user/hello?show=false" match {
  case details (a :: HNil)            => s"details: $a"
  case userInfo(u :: HNil, s :: HNil) => s"user: $u, show: $s"
}
```

## Links
* [ScalaDoc](https://www.javadoc.io/doc/tech.sparse/trail/)

## Licence
Trail is licensed under the terms of the Apache v2.0 licence.

## Authors
* Tim Nieradzik
* Darren Gibson
* Anatolii Kmetiuk
