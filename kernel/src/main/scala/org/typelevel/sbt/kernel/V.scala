package org.typelevel.sbt.kernel

import scala.util.Try

final case class V(
    major: Int,
    minor: Int,
    patch: Option[Int],
    prerelease: Option[String]
) extends Ordered[V] {

  override def toString: String =
    s"$major.$minor${patch.fold("")(p => s".$p")}${prerelease.fold("")(p => s"-$p")}"

  def isPrerelease: Boolean = prerelease.nonEmpty

  def isSameSeries(that: V): Boolean =
    this.major == that.major && this.minor == that.minor

  def mustBeBinCompatWith(that: V): Boolean =
    this >= that && !that.isPrerelease && this.major == that.major && (major > 0 || this.minor == that.minor)

  def compare(that: V): Int = {
    val x = this.major.compare(that.major)
    if (x != 0) return x
    val y = this.minor.compare(that.minor)
    if (y != 0) return y
    (this.patch, that.patch) match {
      case (None, None) => 0
      case (None, Some(patch)) => 1
      case (Some(patch), None) => -1
      case (Some(thisPatch), Some(thatPatch)) =>
        val z = thisPatch.compare(thatPatch)
        if (z != 0) return z
        (this.prerelease, that.prerelease) match {
          case (None, None) => 0
          case (Some(_), None) => 1
          case (None, Some(_)) => -1
          case (Some(x), Some(y)) =>
            // TODO not great, but not everyone uses Ms and RCs
            x.compare(y)
        }
    }
  }

}

object V {
  val version = """^(0|[1-9]\d*)\.(0|[1-9]\d*)(?:\.(0|[1-9]\d*))?(?:-(.+))?$""".r

  def apply(v: String): Option[V] = V.unapply(v)

  def unapply(v: String): Option[V] = v match {
    case version(major, minor, patch, prerelease) =>
      Try(V(major.toInt, minor.toInt, Option(patch).map(_.toInt), Option(prerelease))).toOption
    case _ => None
  }

  object Tag {
    def unapply(v: String): Option[V] =
      if (v.startsWith("v")) V.unapply(v.substring(1)) else None
  }
}
