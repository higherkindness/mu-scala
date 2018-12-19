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

package higherkindness.mu.rpc
package internal
package util

import monocle._
import monocle.function.all._
import scala.Function.{const => κ}
import higherkindness.mu.rpc.protocol.{Avro, AvroWithSchema, Protobuf, SerializationType}

trait AstOptics {

  import Toolbox.u._

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

  val rpcTypeNameFromTypeConstructor: Optional[Tree, Tree] = Optional[Tree, Tree] {
    case ast._Ident(x)                                          => Some(x)
    case ast._SingletonTypeTree(x)                              => Some(x)
    case ast._AppliedTypeTree(AppliedTypeTree(x, List(tpe)))    => Some(tpe)
    case ast._AppliedTypeTree(AppliedTypeTree(x, List(_, tpe))) => Some(tpe)
    case _                                                      => None
  } { name =>
    identity
  }

  val _StreamingConstructor: Prism[Ident, String] = Prism.partial[Ident, String] {
    case Ident(TypeName("Observable")) => "Observable"
    case Ident(TypeName("Stream"))     => "Stream"
  } {
    case "Observable" => Ident(TypeName("Observable"))
    case "Stream"     => Ident(TypeName("Stream"))
  }

  val appliedTypeTreeTpt: Lens[AppliedTypeTree, Tree] =
    Lens[AppliedTypeTree, Tree](_.tpt)(tpt => att => AppliedTypeTree(tpt, att.args))

  val streamingTypeFromConstructor: Optional[Tree, String] = ast._AppliedTypeTree ^|-> appliedTypeTreeTpt ^<-? ast._Ident ^<-? _StreamingConstructor

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

  val firstParamForRpc: Optional[Tree, Tree] = params ^|-? headOption ^|-> valDefTpt ^|-? rpcTypeNameFromTypeConstructor

  val annotationName: Optional[Tree, String] = ast._Select ^|-> qualifier ^<-? ast._New ^|-> newTpt ^|-? name

  val responseStreaming: Optional[DefDef, String] = returnType ^|-? streamingTypeFromConstructor

  val requestStreaming: Optional[Tree, String] = params ^|-? headOption ^|-> valDefTpt ^|-? streamingTypeFromConstructor

  val toAnnotation: Optional[Tree, Annotation] = Optional[Tree, Annotation] {
    case ast._Apply(Apply(fun, Nil)) =>
      annotationName.getOption(fun).map(Annotation.NoParamAnnotation)
    case ast._Apply(Apply(fun, args)) =>
      val namedArgs = args.collect {
        case AssignOrNamedArg(argName, value) => argName.toString -> value
      }

      annotationName.getOption(fun).map { name =>
        if (namedArgs.size == args.size) {
          Annotation.AllNamedArgsAnnotation(name, namedArgs.toMap)
        } else {
          Annotation.UnnamedArgsAnnotation(name, args)
        }
      }

    case _ => None
  } { ann =>
    identity
  }

  val annotations: Lens[Modifiers, List[Tree]] =
    Lens[Modifiers, List[Tree]](_.annotations)(anns =>
      mod => Modifiers(mod.flags, mod.privateWithin, anns))

  val parsedAnnotations: Traversal[Tree, Annotation] = modifiers ^|-> annotations ^|->> each ^|-? toAnnotation

  val _AsIdlType: Prism[Ident, SerializationType] = Prism.partial[Ident, SerializationType] {
    case Ident(TermName("Protobuf"))       => Protobuf
    case Ident(TermName("Avro"))           => Avro
    case Ident(TermName("AvroWithSchema")) => AvroWithSchema
  } {
    case Protobuf       => Ident(TermName("Protobuf"))
    case Avro           => Ident(TermName("Avro"))
    case AvroWithSchema => Ident(TermName("AvroWithSchema"))
  }

  val idlType: Traversal[Tree, SerializationType] =
    annotationsNamed("rpc") ^|-? Annotation.firstArg ^<-? ast._Ident ^<-? _AsIdlType

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
      case Annotation.NoParamAnnotation(_)            => None
      case Annotation.UnnamedArgsAnnotation(_, args)  => args.headOption
      case Annotation.AllNamedArgsAnnotation(_, args) => args.headOption.map(_._2)
    }
    def withArgsNamed(names: String*): Option[Seq[Tree]] = this match {
      case Annotation.NoParamAnnotation(_)            => counted(names, Seq.empty)
      case Annotation.UnnamedArgsAnnotation(_, args)  => counted(names, args)
      case Annotation.AllNamedArgsAnnotation(_, args) => counted(names, names.flatMap(args.get))
    }
    private def counted(names: Seq[String], args: Seq[Tree]): Option[Seq[Tree]] =
      Some(args).filter(_.size >= names.size)
  }

  object Annotation {

    val firstArg: Optional[Annotation, Tree] = Optional[Annotation, Tree](_.firstArg) { x =>
      identity
    }

    case class NoParamAnnotation(name: String)                               extends Annotation
    case class UnnamedArgsAnnotation(name: String, args: Seq[Tree])          extends Annotation
    case class AllNamedArgsAnnotation(name: String, args: Map[String, Tree]) extends Annotation
  }

}

object AstOptics extends AstOptics
