/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

class RPCMethodNameTests extends ScalaCheckSuite with RPCFixtures {

  implicit val ioRuntime: IORuntime = IORuntime.global

  object RPCService {

    case class Request(s: String)

    case class Response(length: Int)

    @service(Protobuf, methodNameStyle = Capitalize) trait ProtoRPCServiceDef[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(Avro, methodNameStyle = Capitalize) trait AvroRPCServiceDef[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(AvroWithSchema, methodNameStyle = Capitalize) trait AvroWithSchemaRPCServiceDef[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }

    class RPCServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCServiceDef[F]
        with AvroRPCServiceDef[F]
        with AvroWithSchemaRPCServiceDef[F] {

      def proto(bd: Request): F[Response]          = Response(bd.s.length).pure
      def avro(bd: Request): F[Response]           = Response(bd.s.length).pure
      def avroWithSchema(bd: Request): F[Response] = Response(bd.s.length).pure
    }

  }

  import RPCService._

  implicit val H: RPCServiceDefImpl[IO] = new RPCServiceDefImpl[IO]

  val protoClientFixture = buildResourceFixture(
    "rpc-proto-client",
    initServerWithClient[ProtoRPCServiceDef[IO]](
      ProtoRPCServiceDef.bindService[IO],
      ProtoRPCServiceDef.clientFromChannel[IO](_)
    )
  )

  val avroClientFixture = buildResourceFixture(
    "rpc-avro-client",
    initServerWithClient[AvroRPCServiceDef[IO]](
      AvroRPCServiceDef.bindService[IO],
      AvroRPCServiceDef.clientFromChannel[IO](_)
    )
  )

  val avroWithSchemaClientFixture = buildResourceFixture(
    "rpc-avro-with-schema-client",
    initServerWithClient[AvroWithSchemaRPCServiceDef[IO]](
      AvroWithSchemaRPCServiceDef.bindService[IO],
      AvroWithSchemaRPCServiceDef.clientFromChannel[IO](_)
    )
  )

  override def munitFixtures =
    List(protoClientFixture, avroClientFixture, avroWithSchemaClientFixture)

  property(
    "A RPC server should " +
      "be able to call a service with a capitalized method using proto"
  ) {
    val client = protoClientFixture()
    forAll { s: String =>
      client.proto(Request(s)).map(_.length).unsafeRunSync() == s.length
    }
  }

  property(
    "A RPC server should " +
      "be able to call a service with a capitalized method using avro"
  ) {
    val client = avroClientFixture()
    forAll { s: String =>
      client.avro(Request(s)).map(_.length).unsafeRunSync() == s.length
    }
  }

  property(
    "A RPC server should " +
      "be able to call a service with a capitalized method using avro with schema"
  ) {
    val client = avroWithSchemaClientFixture()
    forAll { s: String =>
      client.avroWithSchema(Request(s)).map(_.length).unsafeRunSync() == s.length
    }
  }
}
