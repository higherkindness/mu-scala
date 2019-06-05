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

package higherkindness.mu.rpc.internal.encoders

import java.io.{ByteArrayInputStream, InputStream}

import cats.implicits._
import cats.kernel.Monoid
import higherkindness.mu.rpc.internal.encoders.pbd._
import higherkindness.mu.rpc.protocol.ProtoDefault
import io.grpc.MethodDescriptor.Marshaller
import org.scalatest._

class DefaultPBDirectMarshallerTests extends WordSpec with Matchers {

  case class MyTestDefault(s: String)
  private val expectedString = "12345"

  private val emptyInputStream: InputStream = new ByteArrayInputStream(Array())

  private def testDefault[A](expectedDefault: A)(implicit M: Marshaller[A]) =
    M.parse(emptyInputStream) shouldBe expectedDefault

  "Default pbd marshaller" should {

    "handle empty streams by filling in the default value" in {
      testDefault[Float](0.0f)
      testDefault[Double](0.0)
      testDefault[Boolean](false)
      testDefault[Int](0)
      testDefault[Long](0)
      testDefault[String]("")
    }

    "handle empty streams by filling in the default value when a ProtoDefault typeclass exists" in {
      implicit val protoDefault: ProtoDefault[MyTestDefault] = new ProtoDefault[MyTestDefault] {
        override def default: MyTestDefault = MyTestDefault(expectedString)
      }

      testDefault[MyTestDefault](MyTestDefault(expectedString))
    }

    "handle empty streams by filling in the default value when a Monoid typeclass exists" in {
      implicit val monoid: Monoid[MyTestDefault] = new Monoid[MyTestDefault] {
        override def empty: MyTestDefault = MyTestDefault(expectedString)

        override def combine(x: MyTestDefault, y: MyTestDefault): MyTestDefault =
          ??? // explicit as it should never be called
      }

      testDefault[MyTestDefault](MyTestDefault(expectedString))
    }

    "handle empty streams by filling in the default when an automatically derived Monoid typeclass exists" in {
      import cats.derived._
      import auto.monoid._

      testDefault[MyTestDefault](MyTestDefault(""))
    }
  }
}
