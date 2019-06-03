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
import higherkindness.mu.rpc.internal.encoders.pbd._
import io.grpc.MethodDescriptor.Marshaller
import org.scalatest._

class DefaultDirectPBMarshallerTests extends WordSpec with Matchers {

  "Default PBDirect marshaller" should {

    val emptyInputStream: InputStream = new ByteArrayInputStream(Array())

    "handle empty streams by filling in the default value" in {

      implicitly[Marshaller[Float]].parse(emptyInputStream) shouldBe 0.0f

    }

  }
}
