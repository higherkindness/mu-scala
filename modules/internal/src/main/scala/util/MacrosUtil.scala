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

import scala.tools.reflect.ToolBox

class MacrosUtil(val tb: ToolBox[reflect.runtime.universe.type]) {

  import tb.u._

  def isAbstract(cls: ClassDef): Boolean = cls.asInstanceOf[Symbol].isAbstract

  def name(tree: Tree): String = tree match {
    case x: DefDef    => x.name.toString
    case x: TypeDef   => x.name.toString
    case x: ClassDef  => x.name.toString
    case x: ModuleDef => x.name.toString
    case _            => ""
  }

  def typeParams(tree: Tree): Seq[TypeDef] = tree match {
    case x: DefDef   => x.tparams
    case x: TypeDef  => x.tparams
    case x: ClassDef => x.tparams
    case _           => Seq.empty
  }

  def modifiers(tree: Tree): Option[Modifiers] = tree match {
    case x: DefDef    => Some(x.mods)
    case x: ValDef    => Some(x.mods)
    case x: ClassDef  => Some(x.mods)
    case x: ModuleDef => Some(x.mods)
    case _            => None
  }

  def annotations(tree: Tree): Seq[Annotation] = modifiers(tree) match {
    case None => Seq.empty[Annotation]
    case Some(mods) =>
      mods.annotations.collect {
        case Apply(fun, Nil) => NoParamAnnotation(name(tree))
        case Apply(fun, args) =>
          val namedArgs = args.collect {
            case AssignOrNamedArg(argName, value) => argName.toString -> value
          }
          if (namedArgs.size == args.size)
            AllNamedArgsAnnotation(name(tree), namedArgs.toMap) // to lookup by name
          else UnnamedArgsAnnotation(name(tree), args) // to lookup by param order
      }
  }

  def annotationsNamed(tree: Tree)(annName: String): Seq[Annotation] =
    annotations(tree).filter(_.name == annName)

  def hasAnnotation(tree: Tree)(annName: String): Boolean = annotationsNamed(tree)(annName).nonEmpty

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
