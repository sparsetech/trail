# Introduction
Trail is a routing library for Scala. It allows to define type-safe routes, generate URLs and perform pattern matching.

## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "tech.sparse" %%  "trail" % "%version%"  // Scala
libraryDependencies += "tech.sparse" %%% "trail" % "%version%"  // Scala.js, Scala Native
```

## Usage
Create a route:
<listing id="route">

Create a string URL by filling the placeholders of the route:
<listing id="url">

Parse an URL:
<listing id="map">

Define and parse a route with a query parameter:
<listing id="query-params">

A query parameter may be optional:
<listing id="query-params-opt">

A fragment can be specified:
<listing id="query-fragment">

Create a routing table:
<listing id="parse">

Note that you can populate the `trail.Path()` data structure yourself and use it in place of its string counterpart. This is useful if you are using a third-party routing library which already provides you with a deconstructed URL:
<listing id="parse-path">

Define a custom codec that can be used in arguments, parameters and fragments:
<listing id="custom-codec">

Define a custom path element type:
<listing id="custom-path-elem">

Encode and decode [URI values](https://en.wikipedia.org/wiki/Percent-encoding):
<listing id="uri-values">
