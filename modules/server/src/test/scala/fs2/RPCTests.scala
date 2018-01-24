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
package fs2

import freestyle.rpc.common._
import freestyle.rpc.server._
import _root_.fs2.Stream
import freestyle.rpc.server.implicits._
import org.scalatest._

class RPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import freestyle.rpc.fs2.Utils._
  import freestyle.rpc.fs2.Utils.database._
  import freestyle.rpc.fs2.Utils.implicits._

  override protected def beforeAll(): Unit =
    serverStart[ConcurrentMonad].unsafeRunSync()

  override protected def afterAll(): Unit =
    serverStop[ConcurrentMonad].unsafeRunSync()

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

      freesRPCServiceClient.unary(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      val aa: Stream[ConcurrentMonad, C] = freesRPCServiceClient.serverStreaming(b1)

      val bb = aa.compile.toList
      println(s"aa = $aa")
      println(s"bb = $bb")

      bb.unsafeRunSync shouldBe cList

    }
//
//    "be able to run client streaming services" in {
//
//      freesRPCServiceClient
//        .clientStreaming(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
//        .unsafeRunSync() shouldBe dResult
//    }
//
//    "be able to run client bidirectional streaming services" in {
//
//      freesRPCServiceClient
//        .biStreaming(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
//        .compile
//        .toList
//        .unsafeRunSync()
//        .head shouldBe e1
//
//    }

//    "be able to run rpc services concurrently" in {
//
//      val result =
//        (
//          freesRPCServiceClient.unary(a1),
//          freesRPCServiceClient.serverStreaming(b1),
//          freesRPCServiceClient.clientStreaming(
//            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
//          freesRPCServiceClient.biStreaming(
//            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)))
//
//      result.unsafeRunSync() shouldBe ((c1, cList, dResult, e1))
//
//    }

  }

}
