# Introduction
Trail is a routing library for Scala. It allows defining type-safe routes, generating URLs and performing pattern matching.

## Installation
Add the following dependencies to your build configuration:

```scala
libraryDependencies += "tech.sparse" %%  "trail" % "%version%"  // Scala
libraryDependencies += "tech.sparse" %%% "trail" % "%version%"  // Scala.js, Scala Native
```

## Usage
First, import Trail's DSL and define a type-safe route:
<listing id="route">

To fill the route's placeholders, call the `url()` or `apply()` functions:
<listing id="url">

When parsing an URL, Trail maps the values onto Scala types. The result will also contain any unmatched path elements, arguments or the fragment:
<listing id="parse">

We will now define a route with one query parameter. Here, we are only interested in the arguments and exact path matches. For this use case, Trail provides the function `parseArgs()`:
<listing id="query-params">

The output shows that additional arguments are still permitted. If this is undesired, you can call `parseArgsStrict()` to parse a route more strictly:
<listing id="query-params-strict">

Routes may specify optional query parameters:
<listing id="query-params-opt">

You can match fragments, too:
<listing id="query-fragment">

Since `parseArgs()` disallows additional path elements, you can match them only on specific routes using `Elems`. It should be the last DSL combinator in the route definition:
<listing id="additional-elems">

Similarly, additional parameters can be matched with `Params`:
<listing id="additional-params">

Routing tables can be expressed with pattern matching:
<listing id="routing-table">

The underlying `unapply()` function calls `parseArgs()` instead of `parse()`. Therefore, the ordering of routes does not impact precedence.

You may populate the `trail.Path()` data structure yourself and use it in place of an URL. This is useful if an HTTP server already provides the path and arguments of requests:
<listing id="parse-path">

Trail defines codecs for common Scala types. You can define a custom codec for any type. These codecs can be used in arguments, parameters and fragments:
<listing id="custom-codec">

It is possible to define a custom path element type, too:
<listing id="custom-path-elem">

Trail provides helper utilities, for example to encode and decode [URI values](https://en.wikipedia.org/wiki/Percent-encoding):
<listing id="uri-values">
