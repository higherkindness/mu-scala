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

package mu.rpc
package fs2

import mu.rpc.common._
import mu.rpc.server._
import _root_.fs2.Stream
import org.scalatest._

class RPCTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import mu.rpc.fs2.Utils._
  import mu.rpc.fs2.Utils.database._
  import mu.rpc.fs2.Utils.implicits._

  override protected def beforeAll(): Unit =
    serverStart[ConcurrentMonad].unsafeRunSync()

  override protected def afterAll(): Unit =
    serverStop[ConcurrentMonad].unsafeRunSync()

  "mu-rpc server" should {

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

  "mu-rpc client with fs2.Stream as streaming implementation" should {

    "be able to run unary services" in {

      muAvroRPCServiceClient.unary(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schemas" in {

      muAvroWithSchemaRPCServiceClient.unaryWithSchema(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      muProtoRPCServiceClient.serverStreaming(b1).compile.toList.unsafeRunSync() shouldBe cList

    }

    "handle errors in server streaming services" in {

      def clientProgram(errorCode: String): Stream[ConcurrentMonad, C] =
        muProtoRPCServiceClient
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

      muProtoRPCServiceClient
        .clientStreaming(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      muAvroRPCServiceClient
        .biStreaming(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      muAvroWithSchemaRPCServiceClient
        .biStreamingWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      val tuple =
        (
          muAvroRPCServiceClient.unary(a1),
          muAvroWithSchemaRPCServiceClient.unaryWithSchema(a1),
          muProtoRPCServiceClient.serverStreaming(b1),
          muProtoRPCServiceClient.clientStreaming(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          muAvroRPCServiceClient.biStreaming(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          muAvroWithSchemaRPCServiceClient.biStreamingWithSchema(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)))

      tuple._1.unsafeRunSync() shouldBe c1
      tuple._2.unsafeRunSync() shouldBe c1
      tuple._3.compile.toList.unsafeRunSync() shouldBe cList
      tuple._4.unsafeRunSync() shouldBe dResult33
      tuple._5.compile.toList.unsafeRunSync().distinct shouldBe eList
      tuple._6.compile.toList.unsafeRunSync().distinct shouldBe eList

    }

  }

  "mu-rpc client with fs2.Stream as streaming implementation and compression enabled" should {

    "be able to run unary services" in {

      muCompressedAvroRPCServiceClient.unaryCompressed(a1).unsafeRunSync() shouldBe c1

    }

    "be able to run unary services with avro schema" in {

      muCompressedAvroWithSchemaRPCServiceClient
        .unaryCompressedWithSchema(a1)
        .unsafeRunSync() shouldBe c1

    }

    "be able to run server streaming services" in {

      muCompressedProtoRPCServiceClient
        .serverStreamingCompressed(b1)
        .compile
        .toList
        .unsafeRunSync shouldBe cList

    }

    "be able to run client streaming services" in {

      muCompressedProtoRPCServiceClient
        .clientStreamingCompressed(Stream.fromIterator[ConcurrentMonad, A](aList.iterator))
        .unsafeRunSync() shouldBe dResult33
    }

    "be able to run client bidirectional streaming services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      muCompressedAvroRPCServiceClient
        .biStreamingCompressed(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run client bidirectional streaming services with avro schema" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      muCompressedAvroWithSchemaRPCServiceClient
        .biStreamingCompressedWithSchema(Stream.fromIterator[ConcurrentMonad, E](eList.iterator))
        .compile
        .toList
        .unsafeRunSync()
        .distinct shouldBe eList

    }

    "be able to run multiple rpc services" in {

      ignoreOnTravis(
        "TODO: restore once https://github.com/higherkindness/mu-rpc/issues/164 is fixed")

      val tuple =
        (
          muCompressedAvroRPCServiceClient.unaryCompressed(a1),
          muCompressedAvroWithSchemaRPCServiceClient.unaryCompressedWithSchema(a1),
          muCompressedProtoRPCServiceClient.serverStreamingCompressed(b1),
          muCompressedProtoRPCServiceClient.clientStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, A](aList.iterator)),
          muCompressedAvroRPCServiceClient.biStreamingCompressed(
            Stream.fromIterator[ConcurrentMonad, E](eList.iterator)),
          muCompressedAvroWithSchemaRPCServiceClient.biStreamingCompressedWithSchema(
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
