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
import munit.CatsEffectSuite

class RPCTests extends CatsEffectSuite {

  import higherkindness.mu.rpc.fs2.Utils.database._
  import higherkindness.mu.rpc.fs2.Utils.implicits._

  val serverFixture = ResourceSuiteLocalFixture("rpc-server", grpcServer)

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
    serverFixture,
    protoFixture,
    avroFixture,
    avroWSFixture,
    protoCompressedFixture,
    avroCompressedFixture,
    avroWSCompressedFixture
  )

  test("mu-rpc server should allow to startup a server and check if it's alive") {
    IO(serverFixture()).flatMap(_.isShutdown).assertEquals(false)
  }

  test("mu-rpc server should allow to get the port where it's running") {
    IO(serverFixture()).flatMap(_.getPort).assertEquals(SC.port)
  }

  test("mu-rpc client with fs2.Stream be able to run unary services") {
    IO(serverFixture()) *>
      IO(avroFixture())
        .flatMap(_.unary(a1))
        .assertEquals(c1)
  }

  test("mu-rpc client with fs2.Stream be able to run unary services with avro schemas") {
    IO(serverFixture()) *>
      IO(avroWSFixture())
        .flatMap(_.unaryWithSchema(a1))
        .assertEquals(c1)
  }

  test("mu-rpc client with fs2.Stream be able to run server streaming services") {
    IO(serverFixture()) *>
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

    IO(serverFixture()) *>
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
    IO(serverFixture()) *>
      IO(protoFixture())
        .flatMap(_.clientStreaming(Stream.fromIterator[IO](aList.iterator, 1)))
        .assertEquals(dResult33)
  }

  test("mu-rpc client with fs2.Stream be able to run client bidirectional streaming services") {
    IO(serverFixture()) *>
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
    IO(serverFixture()) *>
      IO(avroWSFixture())
        .flatMap(
          _.biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
            .flatMap(_.compile.toList)
        )
        .map(_.distinct)
        .assertEquals(eList)
  }

  test("mu-rpc client with fs2.Stream be able to run multiple rpc services") {
    val tuple =
      (
        IO(avroFixture())
          .flatMap(_.unary(a1)),
        IO(avroWSFixture())
          .flatMap(_.unaryWithSchema(a1)),
        IO(protoFixture())
          .flatMap(_.serverStreaming(b1).flatMap(_.compile.toList)),
        IO(protoFixture())
          .flatMap(
            _.clientStreaming(Stream.fromIterator[IO](aList.iterator, 1))
          ),
        IO(avroFixture())
          .flatMap(
            _.biStreaming(Stream.fromIterator[IO](eList.iterator, 1)).flatMap(_.compile.toList)
          ),
        IO(avroWSFixture())
          .flatMap(
            _.biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
              .flatMap(_.compile.toList)
          )
      )

    IO(serverFixture()) *>
      tuple._1.assertEquals(c1) *>
      tuple._2.assertEquals(c1) *>
      tuple._3.assertEquals(cList) *>
      tuple._4.assertEquals(dResult33) *>
      tuple._5.map(_.distinct).assertEquals(eList) *>
      tuple._6.map(_.distinct).assertEquals(eList)

  }

  test("mu-rpc client with fs2.Stream and compression enabled be able to run unary services") {
    IO(serverFixture()) *>
      IO(avroCompressedFixture())
        .flatMap(_.unaryCompressed(a1))
        .assertEquals(c1)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run unary services with avro schema"
  ) {
    IO(serverFixture()) *>
      IO(avroWSCompressedFixture())
        .flatMap(_.unaryCompressedWithSchema(a1))
        .assertEquals(c1)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run server streaming services"
  ) {
    IO(serverFixture()) *>
      IO(protoCompressedFixture())
        .flatMap(_.serverStreamingCompressed(b1).flatMap(_.compile.toList))
        .assertEquals(cList)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run client streaming services"
  ) {
    IO(serverFixture()) *>
      IO(protoCompressedFixture())
        .flatMap(_.clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1)))
        .assertEquals(dResult33)
  }

  test(
    "mu-rpc client with fs2.Stream and compression enabled be able to run client bidirectional streaming services"
  ) {
    IO(serverFixture()) *>
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
    IO(serverFixture()) *>
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

    val tuple =
      (
        IO(avroCompressedFixture())
          .flatMap(_.unaryCompressed(a1)),
        IO(avroWSCompressedFixture())
          .flatMap(_.unaryCompressedWithSchema(a1)),
        IO(protoCompressedFixture())
          .flatMap(
            _.serverStreamingCompressed(b1).flatMap(_.compile.toList)
          ),
        IO(protoCompressedFixture())
          .flatMap(
            _.clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1))
          ),
        IO(avroCompressedFixture())
          .flatMap(
            _.biStreamingCompressed(Stream.fromIterator[IO](eList.iterator, 1))
              .flatMap(_.compile.toList)
          ),
        IO(avroWSCompressedFixture())
          .flatMap(
            _.biStreamingCompressedWithSchema(
              Stream.fromIterator[IO](eList.iterator, 1)
            ).flatMap(_.compile.toList)
          )
      )

    IO(serverFixture()) *>
      tuple._1.assertEquals(c1) *>
      tuple._2.assertEquals(c1) *>
      tuple._3.assertEquals(cList) *>
      tuple._4.assertEquals(dResult33) *>
      tuple._5.map(_.distinct).assertEquals(eList) *>
      tuple._6.map(_.distinct).assertEquals(eList)
  }

}
