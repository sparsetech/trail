package trail

case class Arg[T]()(implicit val codec: Codec[T]) {
  override def equals(o: Any): Boolean =
    o match {
      case a: Arg[T] => a.codec.equals(codec)
      case _ => false
    }

  override def hashCode(): Int = ("trail.Arg", codec).hashCode()
}

case class Param[T](name: String)(implicit val codec: Codec[T]) {
  override def equals(o: Any): Boolean =
    o match {
      case p: Param[T] => p.name.equals(name) && p.codec.equals(codec)
      case _ => false
    }

  override def hashCode(): Int = ("trail.Param", name, codec).hashCode()
}

case class Fragment[T]()(implicit val codec: Codec[T]) {
  override def equals(o: Any): Boolean =
    o match {
      case f: Fragment[T] => f.codec.equals(codec)
      case _ => false
    }

  override def hashCode(): Int = ("trail.Fragment", codec).hashCode()
}
