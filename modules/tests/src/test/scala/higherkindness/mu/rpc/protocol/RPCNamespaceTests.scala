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

package higherkindness.mu.rpc.protocol

import cats.Applicative
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.applicative._
import higherkindness.mu.rpc.protocol.Utils._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class RPCNamespaceTests extends ScalaCheckSuite with RPCFixtures {

  implicit val ioRuntime: IORuntime = IORuntime.global

  object RPCService {

    case class Request(s: String)

    case class Response(length: Int)

    @service(Protobuf, namespace = Some("my.namespace")) trait ProtoRPCServiceDef[F[_]] {
      def proto1(req: Request): F[Response]
    }
    @service(Avro, namespace = Some("my.namespace")) trait AvroRPCServiceDef[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(AvroWithSchema, namespace = Some("my.namespace")) trait AvroWithSchemaRPCServiceDef[F[
        _
    ]] {
      def avroWithSchema(req: Request): F[Response]
    }

    class RPCServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCServiceDef[F]
        with AvroRPCServiceDef[F]
        with AvroWithSchemaRPCServiceDef[F] {

      def proto1(bd: Request): F[Response]         = Response(bd.s.length).pure
      def avro(bd: Request): F[Response]           = Response(bd.s.length).pure
      def avroWithSchema(bd: Request): F[Response] = Response(bd.s.length).pure
    }

  }

  import RPCService._

  implicit val H: RPCServiceDefImpl[IO] = new RPCServiceDefImpl[IO]

  val protoFixture = buildResourceFixture(
    "rpc-proto-client",
    initServerWithClient[ProtoRPCServiceDef[IO]](
      ProtoRPCServiceDef.bindService[IO],
      ProtoRPCServiceDef.clientFromChannel[IO](_)
    )
  )
  val avroFixture = buildResourceFixture(
    "rpc-avro-client",
    initServerWithClient[AvroRPCServiceDef[IO]](
      AvroRPCServiceDef.bindService[IO],
      AvroRPCServiceDef.clientFromChannel[IO](_)
    )
  )
  val avroWithSchemaFixture = buildResourceFixture(
    "rpc-avro-with-schema-client",
    initServerWithClient[AvroWithSchemaRPCServiceDef[IO]](
      AvroWithSchemaRPCServiceDef.bindService[IO],
      AvroWithSchemaRPCServiceDef.clientFromChannel[IO](_)
    )
  )

  override def munitFixtures = List(protoFixture, avroFixture, avroWithSchemaFixture)

  property("RPC Server should be able to call a service with a defined namespace with proto") {
    val client = protoFixture()
    forAll { s: String =>
      client.proto1(Request(s)).map(_.length).unsafeRunSync() == s.length
    }
  }

  property("RPC Server should be able to call a service with a defined namespace with avro") {
    val client = avroFixture()
    forAll { s: String => client.avro(Request(s)).map(_.length).unsafeRunSync() == s.length }
  }

  property(
    "RPC Server should be able to call a service with a defined namespace with avro with schema"
  ) {
    val client = avroWithSchemaFixture()
    forAll { s: String =>
      client.avroWithSchema(Request(s)).map(_.length).unsafeRunSync() == s.length
    }
  }
}
