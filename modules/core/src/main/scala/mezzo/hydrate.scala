/* -
 * Mezzo [core]
 */

package mezzo

import scala.annotation.tailrec
import scala.reflect.macros.whitebox
import scala.Predef.classOf

import cats._
import cats.instances.all._
import shapeless._

final class Hydrate[D <: Dehydrated] {
  def hydrate[F[_]](implicit alg: Algebra[F]): D#H#Out[F]  =
    macro HydrationMacros.hydrate[D, F]
}

object Hydrate {
  def apply[T <: Dehydrated]: Hydrate[T] = new Hydrate[T]
}

trait Dehydrated {
  type H <: Hydration[_]
}

abstract class Hydration[H <: HydrationMacros](val h: H) {
  type Out[F[_]]

  import h.c.universe._

  def hydrate(F: Symbol, nodes: List[h.NodeInfo]): Tree

  // some misc helpers

  private[this] val UnitTypeSymbol = typeOf[Unit].typeSymbol

  def isSingleton(tpe: Type): Boolean = !tpe.typeSymbol.companion.isModule
  def isUnitType(tpe: Type): Boolean = tpe.typeSymbol match {
    case `UnitTypeSymbol` => false
    case _                => true
  }

}

@macrocompat.bundle
final class HydrationMacros(val c: whitebox.Context) {
  import c.universe._

  def hydrate[D <: Dehydrated, F[_]](alg: c.Expr[Algebra[F]])(
    implicit
      evD: c.WeakTypeTag[D],
      evF: c.WeakTypeTag[F[_]]
  ): c.Expr[D#H#Out[F]] =
    (
      for {
        FSym       <- typeRefToSym(evF.tpe)
        aux        <- typeToAuxType(alg.tree.tpe)
        nodeTypes  <- hlistTypeToTypeList(aux)
        nodeInfos  <- Traverse[List].traverse(nodeTypes)(nodeTypeToNodeInfo)
        hydration  <- locateHydration(evD.tpe)
        _           = scala.Predef.println(
          s"Today's hydration brought to you by ${hydration.getClass}")
      } yield
        hydration.hydrate(FSym, nodeInfos)

    ) fold (
      error => c.abort(c.enclosingPosition, error),
      tree  => c.Expr[D#H#Out[F]](tree))


  import scala.reflect.runtime.{ universe => ru }
  lazy val m = ru.runtimeMirror(getClass.getClassLoader)

  case class NodeInfo(in: Type, name: String, out: Type)

  private[this] val ShapelessSym       = typeOf[HList].typeSymbol.owner
  private[this] val HNilSym            = typeOf[HNil].typeSymbol
  private[this] val HConsSym           = typeOf[shapeless.::[_, _]].typeSymbol
  private[this] val NodeSym            = typeOf[Algebra.Node[_, _, _]].typeSymbol
  private[this] val NodeSymOwner       = typeOf[Algebra.Node[_, _, _]].typeSymbol.owner
  private[this] val MezzoSym           = typeOf[Algebra[Nothing]].typeSymbol.owner
  private[this] val HTypeName          = TypeName("H")

  @tailrec
  final def hlistTypeFoldLeft[A](tpe: Type)(a0: A)(f: (A, Type) => A): Either[String, A] = tpe match {
    case TypeRef(ThisType(ShapelessSym), HNilSym, Nil) => Right(a0)
    case TypeRef(ThisType(ShapelessSym), HConsSym, List(headType, tailType)) =>
      hlistTypeFoldLeft(tailType)(f(a0, headType))(f)
    case _ =>
      Left("Unexpected type when inspecting HList")
  }

  private[this] final def hlistTypeToTypeList(tpe: Type): Either[String, List[Type]] =
    hlistTypeFoldLeft(tpe)(List.empty[Type])((acc, t) => t :: acc)

  def typeToAuxType(tpe: Type): Either[String, Type] = tpe.dealias match {
    case RefinedType(parents, decls) => decls.headOption match {
      case Some(head: TypeSymbol)    => Right(head.toType.dealias)
      case _                         =>
        Left("Unable to extract dependant type from Algebra.Aux")
    }
    case _                           =>
      Left("Expected a refined type")
  }

  private[this] final def nodeTypeToNodeInfo(tpe: Type): Either[String, NodeInfo] = tpe match {
    case TypeRef(ThisType(NodeSymOwner), NodeSym,
      List(in, ConstantType(Constant(name: String)), out)) =>
        Right(NodeInfo(in, name, out))
    case _ => Left(s"unexpected node type $tpe")
  }

  private[this] final def typeRefToSym(tpe: Type): Either[String, Symbol] =
    tpe match {
      case TypeRef(pre, sym: TypeSymbol, Nil) => Right(sym)
      case _                                  =>
        Left(s"unable to extract TypeRef symbol for $tpe")
    }

  private[this] final def locateHydration(tpe: Type): Either[String, Hydration[this.type]] =
    try {
      // couldn't seem to get this to work using only scala reflection, so
      // java is used for a bit of work
      val hType        = tpe.member(HTypeName).asType.toType.dealias.asInstanceOf[ru.Type]
      val hClassSymbol = hType.typeSymbol.asClass
      val hClass       = m.runtimeClass(hClassSymbol).asInstanceOf[Class[Hydration[this.type]]]
      val hydration    = hClass.getDeclaredConstructor(classOf[HydrationMacros]).newInstance(this)

      Right(hydration)
    } catch {
      case e: Throwable => Left(s"unable to instantiate hydration: ${e.getMessage}")
    }

}
