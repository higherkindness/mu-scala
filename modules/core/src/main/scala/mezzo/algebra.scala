/* -
 * Mezzo [core]
 */

package mezzo

import scala.Predef.<:<
import scala.reflect.ClassTag

import shapeless._
import shapeless.labelled._
import shapeless.tag._

sealed trait Algebra[F[_]] {
  type Out <: HList
}

object Algebra {
  type Aux[F[_], O] = Algebra[F] { type Out = O }

  type Node[I, N, O]

  implicit final def algebra[F[_], CF <: Coproduct, LF <: HList, O <: HList](
    implicit
      gen  : LabelledGeneric.Aux[F[_], CF],
      ev   : ops.coproduct.ToHList.Aux[CF, LF],
      build: BuildAlgebra.Aux[F, LF, O]
  ): Algebra.Aux[F, O] =
    new Algebra[F] { type Out = O }

  // @annotation.inductive // leave off until backport/lazy support?
  sealed private[Algebra] abstract class BuildAlgebra[F[_], R] private[this] {
    type Out <: HList
  }

  private[Algebra] object BuildAlgebra {
    type Aux[F[_], R, O] = BuildAlgebra[F, R] { type Out = O }

    implicit final def buildAlgebraHNil[
      F[_]
    ]: BuildAlgebra.Aux[F, HNil, HNil] =
      thereIsNoSpoon

    implicit final def buildAlgebraHCons[
      F[_], A, FA: ? <:< F[A],
      S, K: ? <:< (Symbol @@ S),
      RT <: HList, T <: HList
    ](implicit
        tail: BuildAlgebra.Aux[F, RT, T],
        wit : Witness.Aux[K],
        ctA : ClassTag[A]
    ): BuildAlgebra.Aux[F, FieldType[K, FA] :: RT, Node[FA, S, A] :: T] =
      thereIsNoSpoon

    private[this] final def thereIsNoSpoon[A]: A = null.asInstanceOf[A]
  }

}
