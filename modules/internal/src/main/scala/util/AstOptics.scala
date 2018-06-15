/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle.rpc
package internal
package util

import monocle._
import monocle.function.all._
import scala.Function.{const => κ}
import scala.tools.reflect.ToolBox

class AstOptics(val tb: ToolBox[reflect.runtime.universe.type]) {

  import tb.u._

  object ast {
    val _SingletonTypeTree: Prism[Tree, SingletonTypeTree] = Prism[Tree, SingletonTypeTree] {
      case mod: SingletonTypeTree => Some(mod)
      case _                      => None
    }(identity)

    val _AppliedTypeTree: Prism[Tree, AppliedTypeTree] = Prism[Tree, AppliedTypeTree] {
      case mod: AppliedTypeTree => Some(mod)
      case _                    => None
    }(identity)

    val _ModuleDef: Prism[Tree, ModuleDef] = Prism[Tree, ModuleDef] {
      case mod: ModuleDef => Some(mod)
      case _              => None
    }(identity)

    val _DefDef: Prism[Tree, DefDef] = Prism[Tree, DefDef] {
      case defdef: DefDef => Some(defdef)
      case _              => None
    }(identity)

    def _AnnotatedDefDef(annotationName: String): Prism[Tree, DefDef] =
      Prism[Tree, DefDef] {
        case defdef: DefDef if hasAnnotation(annotationName)(defdef) => Some(defdef)
        case _                                                       => None
      }(identity)

    val _ClassDef: Prism[Tree, ClassDef] = Prism[Tree, ClassDef] {
      case classdef: ClassDef => Some(classdef)
      case _                  => None
    }(identity)

    val _CaseClassDef: Prism[Tree, ClassDef] = Prism[Tree, ClassDef] {
      case classdef: ClassDef if classdef.mods hasFlag Flag.CASE => Some(classdef)
      case _                                                     => None
    }(identity)

    val _ValDef: Prism[Tree, ValDef] = Prism[Tree, ValDef] {
      case valdef: ValDef => Some(valdef)
      case _              => None
    }(identity)

    val _Apply: Prism[Tree, Apply] = Prism[Tree, Apply] {
      case apply: Apply => Some(apply)
      case _            => None
    }(identity)

    val _Select: Prism[Tree, Select] = Prism[Tree, Select] {
      case select: Select => Some(select)
      case _              => None
    }(identity)

    val _New: Prism[Tree, New] = Prism[Tree, New] {
      case n: New => Some(n)
      case _      => None
    }(identity)

    val _Ident: Prism[Tree, Ident] = Prism[Tree, Ident] {
      case ident: Ident => Some(ident)
      case _            => None
    }(identity)
  }

  val fun: Lens[Apply, Tree] = Lens[Apply, Tree](_.fun)(fun => app => Apply(fun, app.args))

  val qualifier: Lens[Select, Tree] =
    Lens[Select, Tree](_.qualifier)(qualifier => select => Select(qualifier, select.name))

  val newTpt: Lens[New, Tree] = Lens[New, Tree](_.tpt)(tpt => n => New(tpt))

  val name: Optional[Tree, String] =
    Optional[Tree, String] {
      case ast._Ident(i)    => Some(i.name.toString)
      case ast._ClassDef(c) => Some(c.name.toString)
      case _                => None
    } { name =>
      {
        case ast._Ident(_)    => Ident(TermName(name))
        case ast._ClassDef(c) => ClassDef(c.mods, TypeName(name), c.tparams, c.impl)
        case x                => x
      }
    }

  /**
   * this Optional needs to handle two different cases:
   *
   * - a type constructor such as `F[T]` (represented by `AppliedTypeTree(TypeName("F"), List(Ident("T")))`)
   * - a type constructor such as `Stream[F, T]` (represented by `AppliedTypeTree(TypeName("Stream"), List(Ident("F"), Ident("T")))`)
   *
   * In both cases, the return should be `T`
   */
  val rpcTypeNameFromTypeConstructor: Optional[Tree, String] = Optional[Tree, String] {
    case ast._Ident(x)                                           => name.getOption(x)
    case ast._SingletonTypeTree(x)                               => Some(x.toString)
    case ast._AppliedTypeTree(AppliedTypeTree(x, List(name)))    => Some(name.toString)
    case ast._AppliedTypeTree(AppliedTypeTree(x, List(_, name))) => Some(name.toString)
    case _                                                       => None
  } { name =>
    {
      case x => x
    }
  }

  val returnType: Lens[DefDef, Tree] = Lens[DefDef, Tree](_.tpt)(tpt =>
    defdef => DefDef(defdef.mods, defdef.name, defdef.tparams, defdef.vparamss, tpt, defdef.rhs))

  val returnTypeAsString = returnType ^|-? rpcTypeNameFromTypeConstructor

  val modifiers: Optional[Tree, Modifiers] = Optional[Tree, Modifiers] {
    case ast._ModuleDef(m) => Some(m.mods)
    case ast._ValDef(m)    => Some(m.mods)
    case ast._ClassDef(m)  => Some(m.mods)
    case ast._DefDef(m)    => Some(m.mods)
    case _                 => None
  } { mods =>
    {
      case ast._ModuleDef(m) => ModuleDef(mods, m.name, m.impl)
      case ast._ValDef(m)    => ValDef(mods, m.name, m.tpt, m.rhs)
      case ast._ClassDef(m)  => ClassDef(mods, m.name, m.tparams, m.impl)
      case ast._DefDef(m)    => DefDef(mods, m.name, m.tparams, m.vparamss, m.tpt, m.rhs)
      case otherwise         => otherwise
    }
  }

  /**
   * There are so many quirks for getting parameters for other
   * constructs such as normal classes.  I think we only need case
   * classes and defs... we'll see :)
   */
  val params: Optional[Tree, List[ValDef]] = Optional[Tree, List[ValDef]] {
    case ast._CaseClassDef(m) =>
      Some(m.impl.collect { case x: ValDef if x.mods hasFlag Flag.CASEACCESSOR => x })
    case ast._DefDef(m) => Some(m.vparamss.flatten)
    case _              => None
  } { κ(identity) }

  val valDefTpt = Lens[ValDef, Tree](_.tpt)(t => v => ValDef(v.mods, v.name, t, v.rhs))

  val firstParamForRpc: Optional[Tree, String] = params ^|-? headOption ^|-> valDefTpt ^|-? rpcTypeNameFromTypeConstructor

  val annotationName: Optional[Tree, String] = ast._Select ^|-> qualifier ^<-? ast._New ^|-> newTpt ^|-? name

  val toAnnotation: Optional[Tree, Annotation] = Optional[Tree, Annotation] {
    case ast._Apply(Apply(fun, Nil)) => annotationName.getOption(fun).map(NoParamAnnotation)
    case ast._Apply(Apply(fun, args)) =>
      val namedArgs = args.collect {
        case AssignOrNamedArg(argName, value) => argName.toString -> value
      }

      annotationName.getOption(fun).map { name =>
        if (namedArgs.size == args.size) {
          AllNamedArgsAnnotation(name, namedArgs.toMap)
        } else {
          UnnamedArgsAnnotation(name, args)
        }
      }

    case _ => None
  } { ann =>
    {
      case ast._Apply(ap) => ???
      case _              => ???
    }
  }

  val annotations: Lens[Modifiers, List[Tree]] =
    Lens[Modifiers, List[Tree]](_.annotations)(anns =>
      mod => Modifiers(mod.flags, mod.privateWithin, anns))

  val parsedAnnotations: Traversal[Tree, Annotation] = modifiers ^|-> annotations ^|->> each ^|-? toAnnotation

  def annotationsNamed(name: String): Traversal[Tree, Annotation] =
    parsedAnnotations ^|-? named(name)

  def hasAnnotation(name: String)(tree: Tree): Boolean =
    annotationsNamed(name).exist(κ(true))(tree)

  def named(name: String): Optional[Annotation, Annotation] =
    Optional[Annotation, Annotation] {
      case x if x.name == name => Some(x)
      case _                   => None
    } { n =>
      identity
    }

  sealed trait Annotation {
    def name: String
    def firstArg: Option[Tree] = this match {
      case NoParamAnnotation(_)            => None
      case UnnamedArgsAnnotation(_, args)  => args.headOption
      case AllNamedArgsAnnotation(_, args) => args.headOption.map(_._2)
    }
    def withArgsNamed(names: String*): Option[Seq[Tree]] = this match {
      case NoParamAnnotation(_)            => counted(names, Seq.empty)
      case UnnamedArgsAnnotation(_, args)  => counted(names, args)
      case AllNamedArgsAnnotation(_, args) => counted(names, names.flatMap(args.get))
    }
    private def counted(names: Seq[String], args: Seq[Tree]): Option[Seq[Tree]] =
      Some(args).filter(_.size >= names.size)
  }
  case class NoParamAnnotation(name: String)                               extends Annotation
  case class UnnamedArgsAnnotation(name: String, args: Seq[Tree])          extends Annotation
  case class AllNamedArgsAnnotation(name: String, args: Map[String, Tree]) extends Annotation

}
