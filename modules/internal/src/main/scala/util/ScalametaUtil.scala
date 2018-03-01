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
      case Annot(Ctor.Ref.Name(name)) => Annotation(name)
      case Annot(Apply(Ctor.Ref.Name(name), args)) =>
        Annotation(name, args.collect {
          case Term.Arg.Named(Term.Name(argName), value) => argName          -> value
          case unnamed                                   => unnamed.toString -> unnamed // single unnamed arg
        }.toMap)
    }

    def annotationsNamed(name: String): Seq[Annotation] = annotations.filter(_.name == name)

    def hasAnnotation(name: String): Boolean = annotationsNamed(name).nonEmpty
  }
}

case class Annotation(name: String, args: Map[String, Term.Arg] = Map.empty)
