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

package higherkindness.mu.rpc
package protocol

import cats.Applicative
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.applicative._
import higherkindness.mu.rpc.protocol.Utils._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class RPCProtoProducts extends ScalaCheckSuite with RPCFixtures {

  implicit val ioRuntime: IORuntime = IORuntime.global

  object RPCService {

    case class MyParam(value: String)

    case class RequestOption(param1: Option[MyParam])

    case class ResponseOption(param1: Option[String], param2: Boolean)

    case class RequestList(param1: List[MyParam])

    case class ResponseList(param1: List[String], param2: Boolean)

    @service(Protobuf)
    trait ProtoRPCServiceDef[F[_]] {
      def optionProto(req: RequestOption): F[ResponseOption]
      def listProto(req: RequestList): F[ResponseList]
    }

    @service(Avro)
    trait AvroRPCServiceDef[F[_]] {
      def optionAvro(req: RequestOption): F[ResponseOption]
      def listAvro(req: RequestList): F[ResponseList]
    }

    @service(AvroWithSchema)
    trait AvroWithSchemaRPCServiceDef[F[_]] {
      def optionAvroWithSchema(req: RequestOption): F[ResponseOption]
      def listAvroWithSchema(req: RequestList): F[ResponseList]
    }

    class RPCServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCServiceDef[F]
        with AvroRPCServiceDef[F]
        with AvroWithSchemaRPCServiceDef[F] {

      def optionProto(req: RequestOption): F[ResponseOption] =
        ResponseOption(req.param1.map(_.value), true).pure
      def optionAvro(req: RequestOption): F[ResponseOption] =
        ResponseOption(req.param1.map(_.value), true).pure
      def optionAvroWithSchema(req: RequestOption): F[ResponseOption] =
        ResponseOption(req.param1.map(_.value), true).pure

      def listProto(req: RequestList): F[ResponseList] =
        ResponseList(req.param1.map(_.value), true).pure
      def listAvro(req: RequestList): F[ResponseList] =
        ResponseList(req.param1.map(_.value), true).pure
      def listAvroWithSchema(req: RequestList): F[ResponseList] =
        ResponseList(req.param1.map(_.value), true).pure
    }

  }

  import RPCService._

  implicit val H: RPCServiceDefImpl[IO] =
    new RPCServiceDefImpl[IO]

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

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Options in the request/response using proto format"
  ) {

    val client = protoFixture()
    forAll { maybeString: Option[String] =>
      // if the string is "", i.e. the protobuf default value,
      // it will not be written on the wire by the client,
      // so the server will decode it as `None`
      val expectedOption = maybeString.filter(_.nonEmpty)
      client
        .optionProto(RequestOption(maybeString.map(MyParam)))
        .unsafeRunSync() == ResponseOption(expectedOption, true)
    }
  }

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Options in the request/response using avro format"
  ) {

    val client = avroFixture()
    forAll { maybeString: Option[String] =>
      client
        .optionAvro(RequestOption(maybeString.map(MyParam)))
        .unsafeRunSync() == ResponseOption(maybeString, true)
    }

  }

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Options in the request/response using avro with schema format"
  ) {

    val client = avroWithSchemaFixture()
    forAll { maybeString: Option[String] =>
      client
        .optionAvroWithSchema(RequestOption(maybeString.map(MyParam)))
        .unsafeRunSync() == ResponseOption(maybeString, true)
    }

  }

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Lists in the request/response using proto format"
  ) {

    val client = protoFixture()
    forAll { list: List[String] =>
      client
        .listProto(RequestList(list.map(MyParam)))
        .unsafeRunSync() == ResponseList(list, true)
    }
  }

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Lists in the request/response using avro format"
  ) {

    val client = avroFixture()
    forAll { list: List[String] =>
      client
        .listAvro(RequestList(list.map(MyParam)))
        .unsafeRunSync() == ResponseList(list, true)
    }
  }

  property(
    "A RPC server should " +
      "be able to serialize and deserialize Lists in the request/response using avro with schema format"
  ) {

    val client = avroWithSchemaFixture()
    forAll { list: List[String] =>
      client
        .listAvroWithSchema(RequestList(list.map(MyParam)))
        .unsafeRunSync() == ResponseList(list, true)
    }
  }

}
