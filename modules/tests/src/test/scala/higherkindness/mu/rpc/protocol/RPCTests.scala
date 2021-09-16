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
package protocol

import cats.effect.IO
import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.protocol.Utils.handlers.client._
import munit.CatsEffectSuite

class RPCTests extends CatsEffectSuite {

  import higherkindness.mu.rpc.protocol.Utils.client.MyRPCClient
  import higherkindness.mu.rpc.protocol.Utils.database._
  import higherkindness.mu.rpc.protocol.Utils.implicits._

  val serverFixture = ResourceSuiteLocalFixture("rpc-server", grpcServer)

  override def munitFixtures = List(serverFixture)

  test("mu server should allow to startup a server and check if it's alive") {
    IO(serverFixture()).flatMap(_.isShutdown).assertEquals(false)
  }

  test("mu server should allow to get the port where it's running") {
    IO(serverFixture()).flatMap(_.getPort).assertEquals(SC.port)
  }

  val muRPCServiceClientHandler: MuRPCServiceClientHandler[IO] =
    new MuRPCServiceClientHandler[IO](
      protoRPCServiceClient,
      avroRPCServiceClient,
      awsRPCServiceClient
    )

  val muRPCServiceClientCompressedHandler: MuRPCServiceClientCompressedHandler[IO] =
    new MuRPCServiceClientCompressedHandler[IO](
      compressedprotoRPCServiceClient,
      compressedavroRPCServiceClient,
      compressedawsRPCServiceClient
    )

  test("mu-client should be able to run unary services") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
      APP.u(a1.x, a1.y)

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(c1)

  }

  test("mu-client should be able to invoke services with empty requests") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.empty

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)

  }

  test("mu-client should #71 issue - empty for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvro

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)
  }

  test("mu-client should empty for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroWithSchema

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)
  }

  test("mu-client should #71 issue - empty response with one param for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroParam(a4)

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)
  }

  test("mu-client should empty response with one param for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroWithSchemaParam(a4)

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)
  }

  test("mu-client should #71 issue - response with empty params for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyAvroParamResponse

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(a4)

  }

  test("mu-client should response with empty params for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyAvroWithSchemaParamResponse

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(a4)

  }

  test("mu-client should #71 issue - empty response with one param for proto") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyParam(a4)

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(Empty)
  }

  test("mu-client should #71 issue - response with empty params for proto") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyParamResponse

    clientProgram[IO](muRPCServiceClientHandler).assertEquals(a4)

  }

  test("mu client with compression - be able to run unary services") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
      APP.u(a1.x, a1.y)

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(c1)

  }

  test("mu client with compression - be able to invoke services with empty requests") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.empty

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)

  }

  test("mu client with compression - #71 issue - empty for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvro

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)
  }

  test("mu client with compression - empty for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroWithSchema

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)
  }

  test("mu client with compression - #71 issue - empty response with one param for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroParam(a4)

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)
  }

  test("mu client with compression - empty response with one param for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyAvroWithSchemaParam(a4)

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)
  }

  test("mu client with compression - #71 issue - response with empty params for avro") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyAvroParamResponse

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(a4)

  }

  test("mu client with compression - response with empty params for avro with schema") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyAvroWithSchemaParamResponse

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(a4)

  }

  test("mu client with compression - #71 issue - empty response with one param for proto") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[Empty.type] =
      APP.emptyParam(a4)

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(Empty)
  }

  test("mu client with compression - #71 issue - response with empty params for proto") {

    def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[A] =
      APP.emptyParamResponse

    clientProgram[IO](muRPCServiceClientCompressedHandler).assertEquals(a4)

  }

}
