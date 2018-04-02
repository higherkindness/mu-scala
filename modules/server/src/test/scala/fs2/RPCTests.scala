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

  "frees-rpc client with fs2.Stream as streaming implementation" should {

    "be able to run unary services" in {

      freesRPCServiceClient.unary(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schemas" in {

      freesRPCServiceClient.unaryWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      freesRPCServiceClient.serverStreaming(b1).compile.toList.unsafeRunSync shouldBe cList

    }

    "be able to run client streaming services" in {

      freesRPCServiceClient
        .clientStreaming(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      freesRPCServiceClient
        .biStreaming(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      freesRPCServiceClient
        .biStreamingWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      val tuple =
        (
          freesRPCServiceClient.unary(a1),
          freesRPCServiceClient.unaryWithSchema(a1),
          freesRPCServiceClient.serverStreaming(b1),
          freesRPCServiceClient.clientStreaming(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          freesRPCServiceClient.biStreaming(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          freesRPCServiceClient.biStreamingWithSchema(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)))

      tuple._1.unsafeRunSync() shouldBe c1
      tuple._2.unsafeRunSync() shouldBe c1
      tuple._3.compile.toList.unsafeRunSync() shouldBe cList
      tuple._4.unsafeRunSync() shouldBe dResult33
      tuple._5.compile.toList.unsafeRunSync().distinct shouldBe eList
      tuple._6.compile.toList.unsafeRunSync().distinct shouldBe eList

    }

  }

  "frees-rpc client with fs2.Stream as streaming implementation and compression enabled" should {

    "be able to run unary services" in {

      freesRPCServiceClient.unaryCompressed(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schema" in {

      freesRPCServiceClient.unaryCompressedWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      freesRPCServiceClient
        .serverStreamingCompressed(b1)
        .compile
        .toList
        .unsafeRunSync shouldBe cList

    }

    "be able to run client streaming services" in {

      freesRPCServiceClient
        .clientStreamingCompressed(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      freesRPCServiceClient
        .biStreamingCompressed(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      freesRPCServiceClient
        .biStreamingCompressedWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      val tuple =
        (
          freesRPCServiceClient.unaryCompressed(a1),
          freesRPCServiceClient.unaryCompressedWithSchema(a1),
          freesRPCServiceClient.serverStreamingCompressed(b1),
          freesRPCServiceClient.clientStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          freesRPCServiceClient.biStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          freesRPCServiceClient.biStreamingCompressedWithSchema(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)))

      tuple._1.unsafeRunSync() shouldBe c1
      tuple._2.unsafeRunSync() shouldBe c1
      tuple._3.compile.toList.unsafeRunSync() shouldBe cList
      tuple._4.unsafeRunSync() shouldBe dResult33
      tuple._5.compile.toList.unsafeRunSync().distinct shouldBe eList
      tuple._6.compile.toList.unsafeRunSync().distinct shouldBe eList

    }

  }

}
