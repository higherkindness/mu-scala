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

      freesAvroRPCServiceClient.unary(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schemas" in {

      freesAvroWithSchemaRPCServiceClient.unaryWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      freesProtoRPCServiceClient.serverStreaming(b1).compile.toList.unsafeRunSync() shouldBe cList

    }

    "handle errors in server streaming services" in {

      def clientProgram(errorCode: String): Stream[ConcurrentMonad, C] =
        freesProtoRPCServiceClient
          .serverStreamingWithError(E(a1, errorCode))
          .handleErrorWith(ex => Stream(C(ex.getMessage, a1)))

      clientProgram("SE").compile.toList
        .unsafeRunSync() shouldBe List(C("INVALID_ARGUMENT: SE", a1))
      clientProgram("SRE").compile.toList
        .unsafeRunSync() shouldBe List(C("INVALID_ARGUMENT: SRE", a1))
      clientProgram("RTE").compile.toList
        .unsafeRunSync() shouldBe List(C("UNKNOWN", a1)) //todo: consider preserving the exception as is done for unary
      clientProgram("Thrown").compile.toList
        .unsafeRunSync() shouldBe List(C("UNKNOWN", a1))
    }

    "be able to run client streaming services" in {

      freesProtoRPCServiceClient
        .clientStreaming(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      freesAvroRPCServiceClient
        .biStreaming(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      freesAvroWithSchemaRPCServiceClient
        .biStreamingWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      val tuple =
        (
          freesAvroRPCServiceClient.unary(a1),
          freesAvroWithSchemaRPCServiceClient.unaryWithSchema(a1),
          freesProtoRPCServiceClient.serverStreaming(b1),
          freesProtoRPCServiceClient.clientStreaming(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          freesAvroRPCServiceClient.biStreaming(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          freesAvroWithSchemaRPCServiceClient.biStreamingWithSchema(
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

      freesAvroRPCServiceClient.unaryCompressed(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schema" in {

      freesAvroWithSchemaRPCServiceClient.unaryCompressedWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      freesProtoRPCServiceClient
        .serverStreamingCompressed(b1)
        .compile
        .toList
        .unsafeRunSync shouldBe cList

    }

    "be able to run client streaming services" in {

      freesProtoRPCServiceClient
        .clientStreamingCompressed(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      freesAvroRPCServiceClient
        .biStreamingCompressed(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      freesAvroWithSchemaRPCServiceClient
        .biStreamingCompressedWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/frees-io/freestyle-rpc/issues/164 is fixed")

      val tuple =
        (
          freesAvroRPCServiceClient.unaryCompressed(a1),
          freesAvroWithSchemaRPCServiceClient.unaryCompressedWithSchema(a1),
          freesProtoRPCServiceClient.serverStreamingCompressed(b1),
          freesProtoRPCServiceClient.clientStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          freesAvroRPCServiceClient.biStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          freesAvroWithSchemaRPCServiceClient.biStreamingCompressedWithSchema(
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
