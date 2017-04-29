[package value="pl.metastack.metarouter.manual"]
# Introduction
MetaRouter is a routing library for Scala and Scala.js. It allows to define type-safe routes, generate URLs and perform pattern matching.

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

Parse an URL:
[scala type="section" value="map" file="Examples"]

Define and parse a route with a query parameter:
[scala type="section" value="query-params" file="Examples"]

A query parameter may be optional:
[scala type="section" value="query-params-opt" file="Examples"]

Create a routing table:
[scala type="section" value="parse" file="Examples"]

Define a custom argument type:
[scala type="section" value="custom-arg" file="Examples"]
