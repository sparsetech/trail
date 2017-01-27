[package value="pl.metastack.metarouter.manual"]
# Introduction
MetaRouter is a routing library for Scala and Scala.js.

It allows to define type-safe routes that can be composed. Furthermore, routes can be mapped to `case class`es.

## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "pl.metastack" %%  "metarouter" % "%version%"  // Scala
libraryDependencies += "pl.metastack" %%% "metarouter" % "%version%"  // Scala.js
```

## Example
Create a route:
[scala type="section" value="route" file="Examples"]

Create a string URL by filling the placeholders of the route:
[scala type="section" value="url" file="Examples"]

Map a route to a `case class` and parse an URL:
[scala type="section" value="map" file="Examples"]

Create a routing table:
[scala type="section" value="parse" file="Examples"]

Create URLs from `case class`es:
[scala type="section" value="urls" file="Examples"]

Shorter with `implicit` keyword:
[scala type="section" value="urls-implicit" file="Examples"]
