/* -
 * Mezzo [core]
 */

package object mezzo {
  private[mezzo] implicit class EitherCompatOps[A, B](val either: Either[A, B]) extends AnyVal {
    def flatMap[AA >: A, C](f: B => Either[AA, C]): Either[AA, C] =
      either.right.flatMap(f)
    def map[AA >: A, C](f: B => C): Either[AA, C] =
      either.right.map(f)
  }
}
