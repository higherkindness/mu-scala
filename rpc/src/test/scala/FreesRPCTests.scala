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

import freestyle._
import org.scalatest._
import freestyle.rpc.server.GrpcServerApp
import freestyle.rpc.Utils.clientProgram.MyRPCClient

class FreesRPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import cats.implicits._
  import freestyle.implicits._
  import freestyle.loggingJVM.implicits._
  import freestyle.rpc.Utils.service._
  import freestyle.rpc.Utils.database._
  import freestyle.rpc.Utils.helpers._
  import freestyle.rpc.Utils.implicits._

  override protected def beforeAll(): Unit =
    serverStart[GrpcServerApp.Op].runF

  override protected def afterAll(): Unit =
    serverStop[GrpcServerApp.Op].runF

  "frees-rpc server" should {

    "allow to startup a server and check if it's alive" in {

      def check[M[_]](implicit APP: GrpcServerApp[M]): FreeS[M, Boolean] =
        APP.server.isShutdown

      check[GrpcServerApp.Op].runF shouldBe false

    }

    "allow to get the port where it's running" in {

      def check[M[_]](implicit APP: GrpcServerApp[M]): FreeS[M, Int] =
        APP.server.getPort

      check[GrpcServerApp.Op].runF shouldBe port

    }

  }

  "frees-rpc client" should {

    "be able to run unary services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, C] =
        APP.u(a1.x, a1.y)

      clientProgram[MyRPCClient.Op].runF shouldBe c1

    }

    "be able to run server streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, List[C]] =
        APP.ss(a2.x, a2.y)

      clientProgram[MyRPCClient.Op].runF shouldBe cList

    }

    "be able to run client streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, D] =
        APP.cs(cList, i)

      clientProgram[MyRPCClient.Op].runF shouldBe dResult
    }

    "be able to run client bidirectional streaming services" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, E] =
        APP.bs(eList)

      clientProgram[MyRPCClient.Op].runF shouldBe e1

    }

    "be able to run rpc services monadically" in {

      def clientProgram[M[_]](implicit APP: MyRPCClient[M]): FreeS[M, (C, List[C], D, E)] = {
        for {
          w <- APP.u(a1.x, a1.y)
          x <- APP.ss(a2.x, a2.y)
          y <- APP.cs(cList, i)
          z <- APP.bs(eList)
        } yield (w, x, y, z)
      }

      clientProgram[MyRPCClient.Op].runF shouldBe ((c1, cList, dResult, e1))

    }

  }

}
