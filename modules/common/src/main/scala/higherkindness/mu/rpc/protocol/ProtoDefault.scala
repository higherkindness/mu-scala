/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.protocol

import alleycats.{Empty => CEmpty}

trait ProtoDefault[A] {
  def default: A
}

object ProtoDefault {

  def apply[A](implicit P: ProtoDefault[A]): ProtoDefault[A] = P

  implicit def protoDefaultFromEmpty[A: CEmpty]: ProtoDefault[A] =
    new ProtoDefault[A] { def default: A = CEmpty[A].empty }

  implicit val protoDefaultMuEmpty: ProtoDefault[Empty.type] = new ProtoDefault[Empty.type] {
    override def default: Empty.type = Empty
  }
}

package object implicits extends EmptyImplicits

trait EmptyImplicits {
  implicit val boolEmpty: CEmpty[Boolean] = new CEmpty[Boolean] {
    override def empty: Boolean = false
  }

  implicit val bigDecimalEmpty: CEmpty[BigDecimal] = new CEmpty[BigDecimal] {
    override def empty: BigDecimal = BigDecimal(0)
  }
}
