/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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
package server

import cats.Id
import freestyle.rpc.server.implicits._
import io.grpc.Server

import scala.concurrent.ExecutionContext

class SyntaxTests extends RpcServerTestSuite {

  import implicits._

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "server syntax" should {

    "allow using bootstrapM" in {

      server[GrpcServerApp.Op].bootstrapM[Id] shouldBe ((): Unit)

      (serverMock.start _: () => Server).verify().once()
      (serverMock.awaitTermination _: () => Unit).verify().once()
    }

  }

}
