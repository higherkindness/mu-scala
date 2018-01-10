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

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.scalatest._
import freestyle.rpc.common._
import freestyle.rpc.server._
import freestyle.rpc.Utils.clientProgram.MyRPCClient
import freestyle.rpc.protocol.Empty

class RPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import freestyle.rpc.Utils._
  import freestyle.rpc.Utils.database._
  import freestyle.rpc.Utils.implicits._

  override protected def beforeAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStart[ConcurrentMonad].unsafeRunSync()
  }

  override protected def afterAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStop[ConcurrentMonad].unsafeRunSync()
  }

  "frees-rpc server" should {

    import freestyle.rpc.server.implicits._

    "allow to startup a server and check if it's alive" in {

      def check[F[_]](implicit S: GrpcServer[F]): F[Boolean] =
        S.isShutdown

      check[ConcurrentMonad].unsafeRunSync() shouldBe false

    }

    "allow to get the port where it's running" in {

      def check[F[_]](implicit S: GrpcServer[F]): F[Int] =
        S.getPort

      check[ConcurrentMonad].unsafeRunSync() shouldBe SC.port

    }

  }

  "frees-rpc client" should {

    "be able to run unary services" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe cList

    }

    "be able to run client streaming services" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe dResult
    }

    "be able to run client bidirectional streaming services" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe e1

    }

    "be able to run rpc services monadically" in {

      def clientProgram[F[_]: Monad](implicit APP: MyRPCClient[F]): F[(C, List[C], D, E)] = {
        for {
          w <- APP.u(a1.x, a1.y)
          x <- APP.ss(a2.x, a2.y)
          y <- APP.cs(cList, i)
          z <- APP.bs(eList)
        } yield (w, x, y, z)
      }

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe ((c1, cList, dResult, e1))

    }

    "#67 issue - booleans as request are not allowed" ignore {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.notAllowed(true)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe c1

    }

    "be able to invoke services with empty requests" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
        APP.empty

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe Empty

    }

    "#71 issue - empty for avro" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
        APP.emptyAvro

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe Empty
    }

    "#71 issue - empty response with one param for avro" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
        APP.emptyAvroParam(a4)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe Empty
    }

    "#71 issue - response with empty params for avro" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
        APP.emptyAvroParamResponse

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe a4

    }

    "#71 issue - empty response with one param for proto" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
        APP.emptyParam(a4)

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe Empty
    }

    "#71 issue - response with empty params for proto" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
        APP.emptyParamResponse

      clientProgram[ConcurrentMonad].unsafeRunSync() shouldBe a4

    }

  }

}
