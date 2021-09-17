/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc
package fs2

import cats.effect.IO
import higherkindness.mu.rpc.common._
import _root_.fs2.Stream
import higherkindness.mu.rpc.server.GrpcServer
import munit.CatsEffectSuite

import scala.concurrent.duration._

class RPCTests extends CatsEffectSuite {

  import higherkindness.mu.rpc.fs2.Utils.database._
  import higherkindness.mu.rpc.fs2.Utils.implicits._

  var grpcServerStarted: Option[GrpcServer[IO]] = None
  var shutdown: Option[IO[Unit]]                = None

  override def beforeAll(): Unit = {
    val tuple = grpcServer.allocated.unsafeRunSync()
    grpcServerStarted = Some(tuple._1)
    shutdown = Some(tuple._2)
  }

  override def afterAll(): Unit =
    shutdown.foreach(_.unsafeRunSync())

  val protoFixture = ResourceSuiteLocalFixture("proto-client", muProtoRPCServiceClient)
  val avroFixture  = ResourceSuiteLocalFixture("avro-client", muAvroRPCServiceClient)
  val avroWSFixture =
    ResourceSuiteLocalFixture("avro-with-schema-client", muAvroWithSchemaRPCServiceClient)
  val protoCompressedFixture =
    ResourceSuiteLocalFixture("proto-compressed-client", muCompressedProtoRPCServiceClient)
  val avroCompressedFixture =
    ResourceSuiteLocalFixture("avro-compressed-client", muCompressedAvroRPCServiceClient)
  val avroWSCompressedFixture = ResourceSuiteLocalFixture(
    "avro-with-schema-compressed-client",
    muCompressedAvroWithSchemaRPCServiceClient
  )

  override def munitFixtures = List(
    protoFixture,
    avroFixture,
    avroWSFixture,
    protoCompressedFixture,
    avroCompressedFixture,
    avroWSCompressedFixture
  )

  test("mu-rpc server should allow to startup a server and check if it's alive") {
    IO
      .fromOption(grpcServerStarted)(new IllegalStateException("Server not initialized"))
      .flatMap(_.isShutdown)
      .assertEquals(false)
  }

  test("mu-rpc server should allow to get the port where it's running") {
    IO
      .fromOption(grpcServerStarted)(new IllegalStateException("Server not initialized"))
      .flatMap(_.getPort)
      .assertEquals(SC.port)
  }

  test("mu-rpc client with fs2.Stream be able to run unary services") {
    IO(avroFixture())
      .flatMap(_.unary(a1))
      .assertEquals(c1)
  }

  test("mu-rpc client with fs2.Stream be able to run unary services with avro schemas") {
    IO(avroWSFixture())
      .flatMap(_.unaryWithSchema(a1))
      .assertEquals(c1)
  }

  test("mu-rpc client with fs2.Stream be able to run server streaming services") {
    IO(protoFixture())
      .flatMap(_.serverStreaming(b1).flatMap(_.compile.toList))
      .assertEquals(cList)
  }

  test("mu-rpc client with fs2.Stream handle errors in server streaming services") {

    def clientProgram(errorCode: String): IO[List[C]] =
      IO(protoFixture())
        .flatMap(
          _.serverStreamingWithError(E(a1, errorCode))
            .map(_.handleErrorWith(ex => Stream(C(ex.getMessage, a1))))
            .flatMap(_.compile.toList)
        )

    clientProgram("SE")
      .assertEquals(List(C("INVALID_ARGUMENT: SE", a1))) *>
      clientProgram("SRE")
        .assertEquals(List(C("INVALID_ARGUMENT: SRE", a1))) *>
      clientProgram("RTE")
        .assertEquals(List(C("INTERNAL: RTE", a1))) *>
      clientProgram("Thrown")
        .assertEquals(List(C("INTERNAL: Thrown", a1)))
  }

  test("mu-rpc client with fs2.Stream be able to run client streaming services") {
    IO(protoFixture())
      .flatMap(_.clientStreaming(Stream.fromIterator[IO](aList.iterator, 1)))
      .assertEquals(dResult33)
  }

  test("mu-rpc client with fs2.Stream be able to run client bidirectional streaming services") {
    IO(avroFixture())
      .flatMap(
        _.biStreaming(Stream.fromIterator[IO](eList.iterator, 1)).flatMap(_.compile.toList)
      )
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(
    "mu-rpc client with fs2.Stream be able to run client bidirectional streaming services with avro schema"
  ) {
    IO(avroWSFixture())
      .flatMap(
        _.biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .map(_.distinct)
      .assertEquals(eList)
  }

  test("mu-rpc client with fs2.Stream be able to run multiple rpc services") {
    val opTimeout: FiniteDuration = 10.seconds
    for {
      avroC <- IO(avroFixture())
      _ <- avroC
        .unary(a1)
        .assertEquals(c1)
        .timeout(opTimeout)
      _ <- avroC
        .biStreaming(Stream.fromIterator[IO](eList.iterator, 1))
        .flatMap(_.compile.toList)
        .map(_.distinct)
        .assertEquals(eList)
        .timeout(opTimeout)

      avroWSC <- IO(avroWSFixture())
      _ <- avroWSC
        .unaryWithSchema(a1)
        .assertEquals(c1)
        .timeout(opTimeout)
      _ <- avroWSC
        .biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
        .flatMap(_.compile.toList)
        .map(_.distinct)
        .assertEquals(eList)
        .timeout(opTimeout)

      protoC <- IO(protoFixture())
      _ <- protoC
        .serverStreaming(b1)
        .flatMap(_.compile.toList)
        .assertEquals(cList)
        .timeout(opTimeout)
      _ <- protoC
        .clientStreaming(Stream.fromIterator[IO](aList.iterator, 1))
        .assertEquals(dResult33)
        .timeout(opTimeout)
    } yield ()
  }

  test("mu-rpc client with fs2.Stream and compression enabled be able to run unary services") {
    IO(avroCompressedFixture())
      .flatMap(_.unaryCompressed(a1))
      .assertEquals(c1)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run unary services with avro schema"
  ) {
    IO(avroWSCompressedFixture())
      .flatMap(_.unaryCompressedWithSchema(a1))
      .assertEquals(c1)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run server streaming services"
  ) {
    IO(protoCompressedFixture())
      .flatMap(_.serverStreamingCompressed(b1).flatMap(_.compile.toList))
      .assertEquals(cList)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run client streaming services"
  ) {
    IO(protoCompressedFixture())
      .flatMap(_.clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1)))
      .assertEquals(dResult33)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run client bidirectional streaming services"
  ) {
    IO(avroCompressedFixture())
      .flatMap(
        _.biStreamingCompressed(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run client bidirectional streaming services with avro schema"
  ) {
    IO(avroWSCompressedFixture())
      .flatMap(
        _.biStreamingCompressedWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run multiple rpc services"
  ) {
    val opTimeout: FiniteDuration = 10.seconds
    for {
      avroC <- IO(avroCompressedFixture())
      _ <- avroC
        .unaryCompressed(a1)
        .assertEquals(c1)
        .timeout(opTimeout)
      _ <- avroC
        .biStreamingCompressed(Stream.fromIterator[IO](eList.iterator, 1))
        .flatMap(_.compile.toList)
        .map(_.distinct)
        .assertEquals(eList)
        .timeout(opTimeout)

      avroWSC <- IO(avroWSCompressedFixture())
      _ <- avroWSC
        .unaryCompressedWithSchema(a1)
        .assertEquals(c1)
        .timeout(opTimeout)
      _ <- avroWSC
        .biStreamingCompressedWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
        .flatMap(_.compile.toList)
        .map(_.distinct)
        .assertEquals(eList)
        .timeout(opTimeout)

      protoC <- IO(protoCompressedFixture())
      _ <- protoC
        .serverStreamingCompressed(b1)
        .flatMap(_.compile.toList)
        .assertEquals(cList)
        .timeout(opTimeout)
      _ <- protoC
        .clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1))
        .assertEquals(dResult33)
        .timeout(opTimeout)
    } yield ()
  }

}
