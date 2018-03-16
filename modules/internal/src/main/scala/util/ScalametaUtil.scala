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

import scala.meta.Mod.Annot
import scala.meta.Term.Apply
import scala.meta._

object ScalametaUtil {

  implicit class DefnOps(val defn: Defn) extends AnyVal {

    def name: String = defn match {
      case x: Defn.Def    => x.name.value
      case x: Defn.Macro  => x.name.value
      case x: Defn.Type   => x.name.value
      case x: Defn.Class  => x.name.value
      case x: Defn.Trait  => x.name.value
      case x: Defn.Object => x.name.value
      case _              => ""
    }

    def typeParams: Seq[Type.Param] = defn match {
      case x: Defn.Def   => x.tparams
      case x: Defn.Macro => x.tparams
      case x: Defn.Type  => x.tparams
      case x: Defn.Class => x.tparams
      case x: Defn.Trait => x.tparams
      case _             => Seq.empty
    }

    def params: Seq[Term.Param] = defn match {
      case x: Defn.Def   => x.paramss.flatten
      case x: Defn.Class => x.ctor.paramss.flatten
      case x: Defn.Trait => x.ctor.paramss.flatten
      case _             => Seq.empty
    }

    def modifiers: Seq[Mod] = defn match {
      case x: Defn.Val    => x.mods
      case x: Defn.Var    => x.mods
      case x: Defn.Def    => x.mods
      case x: Defn.Macro  => x.mods
      case x: Defn.Type   => x.mods
      case x: Defn.Class  => x.mods
      case x: Defn.Trait  => x.mods
      case x: Defn.Object => x.mods
      case _              => Seq.empty
    }

    def annotations: Seq[Annotation] = modifiers.collect {
      case Annot(Ctor.Ref.Name(name)) => NoParamAnnotation(name)
      case Annot(Apply(Ctor.Ref.Name(name), args)) =>
        val namedArgs = args.collect {
          case Term.Arg.Named(Term.Name(argName), value) => argName -> value
        }
        if (namedArgs.size == args.size)
          AllNamedArgsAnnotation(name, namedArgs.toMap) // to lookup by name
        else UnnamedArgsAnnotation(name, args) // to lookup by param order
    }

    def annotationsNamed(name: String): Seq[Annotation] = annotations.filter(_.name == name)

    def hasAnnotation(name: String): Boolean = annotationsNamed(name).nonEmpty
  }
}

sealed trait Annotation {
  def name: String
  def firstArg: Option[Term.Arg] = this match {
    case NoParamAnnotation(_)            => None
    case UnnamedArgsAnnotation(_, args)  => args.headOption
    case AllNamedArgsAnnotation(_, args) => args.headOption.map(_._2)
  }
  def withArgsNamed(names: String*): Option[Seq[Term.Arg]] = this match {
    case NoParamAnnotation(_)            => counted(names, Seq.empty)
    case UnnamedArgsAnnotation(_, args)  => counted(names, args)
    case AllNamedArgsAnnotation(_, args) => counted(names, names.flatMap(args.get))
  }
  private def counted(names: Seq[String], args: Seq[Term.Arg]): Option[Seq[Term.Arg]] =
    Some(args).filter(_.size >= names.size)
}
case class NoParamAnnotation(name: String)                                   extends Annotation
case class UnnamedArgsAnnotation(name: String, args: Seq[Term.Arg])          extends Annotation
case class AllNamedArgsAnnotation(name: String, args: Map[String, Term.Arg]) extends Annotation
