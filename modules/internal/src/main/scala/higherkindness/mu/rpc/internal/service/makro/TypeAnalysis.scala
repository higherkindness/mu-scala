package higherkindness.mu.rpc.internal.service.makro

import scala.reflect.macros.blackbox.Context

class TypeAnalysis[C <: Context](val c: C) {
  import c.universe._

  /**
   * @originalType The original type written by the user.
   *               For a request type, this will be e.g. `MyRequest` or `Stream[F, MyRequest]`
   *               or `Observable[MyRequest]`.
   *               For a response type, it will be inside an effect type,
   *               e.g. `F[MyResponse]` or `F[Stream[F, MyResponse]]` or `F[Observable[MyResponse]]`
   *
   * @unwrappedType The type, with any surrounding effect type stripped.
   *                e.g. `MyRequest` or `Stream[F, MyRequest]` or `Observable[MyRequest]`
   *                For a request type, `unwrappedType` == `originalType`.
   *
   * @messageType The type of the message in a request/response.
   *              For non-streaming request/responses, `messageType` == `unwrappedType`.
   *              For streaming request/responses, `messageType` is the type of the stream elements.
   *              e.g. if `originalType` is `F[Stream[F, MyResponse]]` then `messageType` is `MyResponse`.
   */
  abstract class TypeTypology(
      val originalType: Tree,
      val unwrappedType: Tree,
      val messageType: Tree
  ) extends Product
      with Serializable {

    def isEmpty: Boolean = this match {
      case _: EmptyTpe => true
      case _           => false
    }

    def isStreaming: Boolean = this match {
      case _: Fs2StreamTpe       => true
      case _: MonixObservableTpe => true
      case _                     => false
    }
  }
  object TypeTypology {

    /**
     * Extract the parts of a possibly-qualified type name or term name.
     *
     * {{{
     * extractName(tq"Observable") == List("Observable")
     * extractName(tq"monix.reactive.Observable") == List("monix", "reactive", "Observable")
     * }}}
     */
    private def extractName(tree: Tree): List[String] = tree match {
      case Ident(TermName(name))             => List(name)
      case Ident(TypeName(name))             => List(name)
      case Select(qualifier, TypeName(name)) => extractName(qualifier) :+ name
      case Select(qualifier, TermName(name)) => extractName(qualifier) :+ name
      case _                                 => Nil
    }

    /**
     * Is the given tree a type name or term name that matches the given
     * fully-qualified name?
     *
     * {{{
     * val fqn = List("_root_", "monix", "reactive", "Observable")
     *
     * possiblyQualifiedName(tq"Observable", fqn) == true
     * possiblyQualifiedName(tq"reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"monix.reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"_root_.monix.reactive.Observable", fqn) == true
     * possiblyQualifiedName(tq"com.mylibrary.Observable", fqn) == false
     * }}}
     */
    private def possiblyQualifiedName(tree: Tree, fqn: List[String]): Boolean = {
      val name = extractName(tree)
      name.nonEmpty && fqn.endsWith(name)
    }

    private val monixObservableFQN = List("_root_", "monix", "reactive", "Observable")
    private val fs2StreamFQN       = List("_root_", "fs2", "Stream")

    def apply(t: Tree, responseType: Boolean, F: TypeName): TypeTypology = {
      val unwrappedType: Tree = {
        if (responseType) {
          // Check that the type is wrapped in F as it should be, and unwrap it
          t match {
            case tq"$f[$tparam]" if f.toString == F.decodedName.toString => tparam
            case _ =>
              c.abort(
                t.pos,
                "Invalid RPC response type. All response types should have the shape F[...], where F[_] is the service's type parameter."
              )
          }
        } else {
          // Request type is not wrapped in F[...], so return it as-is
          t
        }
      }

      unwrappedType match {
        case tq"$tpe[$elemType]" if possiblyQualifiedName(tpe, monixObservableFQN) =>
          MonixObservableTpe(t, unwrappedType, elemType)
        case tq"$tpe[$effectType, $elemType]"
            if possiblyQualifiedName(tpe, fs2StreamFQN) && effectType.toString == F.decodedName.toString =>
          Fs2StreamTpe(t, unwrappedType, elemType)
        case tq"Empty.type" => EmptyTpe(t, unwrappedType, unwrappedType)
        case other          => UnaryTpe(t, unwrappedType, unwrappedType)
      }

    }

  }

  case class EmptyTpe(orig: Tree, unwrapped: Tree, message: Tree)
      extends TypeTypology(orig, unwrapped, message)

  case class UnaryTpe(orig: Tree, unwrapped: Tree, message: Tree)
      extends TypeTypology(orig, unwrapped, message)

  case class Fs2StreamTpe(orig: Tree, unwrapped: Tree, streamElem: Tree)
      extends TypeTypology(orig, unwrapped, streamElem)

  case class MonixObservableTpe(orig: Tree, unwrapped: Tree, streamElem: Tree)
      extends TypeTypology(orig, unwrapped, streamElem)

}
