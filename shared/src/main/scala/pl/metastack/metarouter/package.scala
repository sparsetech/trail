package pl.metastack

package object metarouter {
  val Root = Route.Root
  val !#   = Root

  type Arg[T] = Arg_[T]
}
