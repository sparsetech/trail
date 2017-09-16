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
%code(route)

Create a string URL by filling the placeholders of the route:
%code(url)

Parse an URL:
%code(map)

Define and parse a route with a query parameter:
%code(query-params)

A query parameter may be optional:
%code(query-params-opt)

Create a routing table:
%code(parse)

Define a custom argument type:
%code(custom-arg)

Define a custom path element type:
%code(custom-path-elem)
