# Introduction
Trail is a routing library for Scala and Scala.js. It allows to define type-safe routes, generate URLs and perform pattern matching.

## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "tech.sparse" %%  "trail" % "%version%"  // Scala
libraryDependencies += "tech.sparse" %%% "trail" % "%version%"  // Scala.js
```

## Example
Create a route:
[scala block="route"]

Create a string URL by filling the placeholders of the route:
[scala block="url"]

Parse an URL:
[scala block="map"]

Define and parse a route with a query parameter:
[scala block="query-params"]

A query parameter may be optional:
[scala block="query-params-opt"]

Create a routing table:
[scala block="parse"]

Define a custom argument type:
[scala block="custom-arg"]

Define a custom path element type:
[scala block="custom-path-elem"]
