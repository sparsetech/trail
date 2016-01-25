package pl.metastack

import pl.metastack.metarouter.Route.ConvertArgs
import shapeless.HNil
import shapeless.ops.hlist.FlatMapper
import shapeless.ops.hlist.FlatMapper._

package object metarouter {
  val Root = Route.Root
  val !# = Root
}
