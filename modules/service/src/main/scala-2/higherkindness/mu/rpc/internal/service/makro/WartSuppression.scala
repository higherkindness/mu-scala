/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.internal.service.makro

import scala.reflect.macros.blackbox.Context

// $COVERAGE-OFF$
trait SuppressWarts[T] {
  def suppressWarts(warts: String*)(t: T): T
}

object SuppressWarts {
  def apply[A](implicit A: SuppressWarts[A]): SuppressWarts[A] = A
}

class WartSuppression[C <: Context](val c: C) {
  import c.universe._

  implicit val suppressWartsOnModifier: SuppressWarts[Modifiers] = new SuppressWarts[Modifiers] {
    def suppressWarts(warts: String*)(mod: Modifiers): Modifiers = {
      val argList = warts.map(ws => s"org.wartremover.warts.$ws")

      Modifiers(
        mod.flags,
        mod.privateWithin,
        q"new _root_.java.lang.SuppressWarnings(_root_.scala.Array(..$argList))" :: mod.annotations
      )
    }
  }

  implicit val suppressWartsOnClassDef: SuppressWarts[ClassDef] = new SuppressWarts[ClassDef] {
    def suppressWarts(warts: String*)(clazz: ClassDef): ClassDef = {
      ClassDef(
        SuppressWarts[Modifiers].suppressWarts(warts: _*)(clazz.mods),
        clazz.name,
        clazz.tparams,
        clazz.impl
      )
    }
  }

  implicit val suppressWartsOnDefDef: SuppressWarts[DefDef] = new SuppressWarts[DefDef] {
    def suppressWarts(warts: String*)(defdef: DefDef): DefDef = {
      DefDef(
        SuppressWarts[Modifiers].suppressWarts(warts: _*)(defdef.mods),
        defdef.name,
        defdef.tparams,
        defdef.vparamss,
        defdef.tpt,
        defdef.rhs
      )
    }
  }

  implicit val suppressWartsOnValDef: SuppressWarts[ValDef] = new SuppressWarts[ValDef] {
    def suppressWarts(warts: String*)(valdef: ValDef): ValDef = {
      ValDef(
        SuppressWarts[Modifiers].suppressWarts(warts: _*)(valdef.mods),
        valdef.name,
        valdef.tpt,
        valdef.rhs
      )
    }
  }

  implicit class SuppressWartsSyntax[A](value: A)(implicit A: SuppressWarts[A]) {
    def suppressWarts(warts: String*): A = A.suppressWarts(warts: _*)(value)
  }

}
// $COVERAGE-ON$
