/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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
import cats.effect.{Async, IO}
import cats.effect.unsafe.IORuntime
import cats.syntax.applicative._
import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged._
import higherkindness.mu.rpc.internal.encoders.avro.bigDecimalTagged.marshallers._
import higherkindness.mu.rpc.internal.encoders.pbd.bigDecimal._
import higherkindness.mu.rpc.protocol.Utils._
import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop._
import shapeless.{tag, Nat}
import shapeless.tag.@@

import scala.math.BigDecimal.RoundingMode

class RPCBigDecimalTests extends ScalaCheckSuite with RPCFixtures {

  implicit val ioRuntime: IORuntime = IORuntime.global

  private[this] def bigDecimalGen(scale: Int): Gen[BigDecimal] =
    Arbitrary
      .arbitrary[BigDecimal]
      .map(_.setScale(scale, RoundingMode.HALF_DOWN))

  implicit val requestPSArb: Arbitrary[RPCService.RequestPS] = Arbitrary {
    for {
      bd1   <- bigDecimalGen(2).map(tag[(Nat._8, Nat._2)][BigDecimal])
      bd2   <- bigDecimalGen(2).map(tag[((Nat._1, Nat._2), Nat._2)][BigDecimal])
      bd3   <- bigDecimalGen(12).map(tag[((Nat._2, Nat._0), (Nat._1, Nat._2))][BigDecimal])
      label <- Gen.alphaStr
    } yield RPCService.RequestPS(bd1, bd2, bd3, label)
  }

  object RPCService {

    case class Request(bigDecimal: BigDecimal, label: String)

    case class Response(bigDecimal: BigDecimal, result: String, check: Boolean)

    case class RequestPS(
        bd1: BigDecimal @@ (Nat._8, Nat._2),
        bd2: BigDecimal @@ ((Nat._1, Nat._2), Nat._2),
        bd3: BigDecimal @@ ((Nat._2, Nat._0), (Nat._1, Nat._2)),
        label: String
    )

    case class ResponsePS(
        bd1: BigDecimal @@ (Nat._8, Nat._2),
        bd2: BigDecimal @@ ((Nat._1, Nat._2), Nat._2),
        bd3: BigDecimal @@ ((Nat._2, Nat._0), (Nat._1, Nat._2)),
        result: String,
        check: Boolean
    )

    @service(Protobuf) trait ProtoRPCServiceDef[F[_]] {
      def bigDecimalProtoWrapper(req: Request): F[Response]
    }
    @service(Avro) trait AvroRPCServiceDef[F[_]] {
      def bigDecimalAvro(bd: BigDecimal @@ (Nat._8, Nat._2)): F[BigDecimal @@ (Nat._8, Nat._2)]
      def bigDecimalAvroWrapper(req: RequestPS): F[ResponsePS]
    }
    @service(AvroWithSchema) trait AvroWithSchemaRPCServiceDef[F[_]] {
      def bigDecimalAvroWithSchema(
          bd: BigDecimal @@ (Nat._8, Nat._2)
      ): F[BigDecimal @@ (Nat._8, Nat._2)]
      def bigDecimalAvroWithSchemaWrapper(req: RequestPS): F[ResponsePS]
    }

    class RPCServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCServiceDef[F]
        with AvroRPCServiceDef[F]
        with AvroWithSchemaRPCServiceDef[F] {

      def bigDecimalProtoWrapper(req: Request): F[Response] =
        Response(req.bigDecimal, req.label, check = true).pure

      def bigDecimalAvro(bd: BigDecimal @@ (Nat._8, Nat._2)): F[BigDecimal @@ (Nat._8, Nat._2)] =
        bd.pure

      def bigDecimalAvroWrapper(req: RequestPS): F[ResponsePS] =
        ResponsePS(req.bd1, req.bd2, req.bd3, req.label, check = true).pure

      def bigDecimalAvroWithSchema(
          bd: BigDecimal @@ (Nat._8, Nat._2)
      ): F[BigDecimal @@ (Nat._8, Nat._2)] = bd.pure

      def bigDecimalAvroWithSchemaWrapper(req: RequestPS): F[ResponsePS] =
        ResponsePS(req.bd1, req.bd2, req.bd3, req.label, check = true).pure
    }

  }

  object RPCServiceWithImplicitRM {

    implicit val RM: RoundingMode.RoundingMode = RoundingMode.HALF_DOWN

    case class Request(bigDecimal: BigDecimal @@ (Nat._8, Nat._2), label: String)

    case class Response(bigDecimal: BigDecimal @@ (Nat._8, Nat._2), result: String, check: Boolean)

    @service(Avro) trait AvroRPCServiceDef[F[_]] {
      def bigDecimalAvro(bd: BigDecimal @@ (Nat._8, Nat._2)): F[BigDecimal @@ (Nat._8, Nat._2)]
      def bigDecimalAvroWrapper(req: Request): F[Response]
    }

    class RPCServiceDefImpl[F[_]: Applicative] extends AvroRPCServiceDef[F] {

      def bigDecimalAvro(bd: BigDecimal @@ (Nat._8, Nat._2)): F[BigDecimal @@ (Nat._8, Nat._2)] =
        bd.pure

      def bigDecimalAvroWrapper(req: Request): F[Response] =
        Response(req.bigDecimal, req.label, check = true).pure
    }

  }

  val protoFixture = buildResourceFixture(
    "rpc-proto-client",
    initServerWithClient[RPCService.ProtoRPCServiceDef[IO]](
      RPCService.ProtoRPCServiceDef
        .bindService[IO](Async[IO], new RPCService.RPCServiceDefImpl[IO]),
      RPCService.ProtoRPCServiceDef.clientFromChannel[IO](_)
    )
  )
  val avroFixture = buildResourceFixture(
    "rpc-avro-client",
    initServerWithClient[RPCService.AvroRPCServiceDef[IO]](
      RPCService.AvroRPCServiceDef.bindService[IO](Async[IO], new RPCService.RPCServiceDefImpl[IO]),
      RPCService.AvroRPCServiceDef.clientFromChannel[IO](_)
    )
  )
  val avroWithSchemaFixture = buildResourceFixture(
    "rpc-avro-with-schema-client",
    initServerWithClient[RPCService.AvroWithSchemaRPCServiceDef[IO]](
      RPCService.AvroWithSchemaRPCServiceDef
        .bindService[IO](Async[IO], new RPCService.RPCServiceDefImpl[IO]),
      RPCService.AvroWithSchemaRPCServiceDef.clientFromChannel[IO](_)
    )
  )
  val avroRMFixture = buildResourceFixture(
    "rpc-avro-client",
    initServerWithClient[RPCServiceWithImplicitRM.AvroRPCServiceDef[IO]](
      RPCServiceWithImplicitRM.AvroRPCServiceDef
        .bindService[IO](Async[IO], new RPCServiceWithImplicitRM.RPCServiceDefImpl[IO]),
      RPCServiceWithImplicitRM.AvroRPCServiceDef.clientFromChannel[IO](_)
    )
  )

  override def munitFixtures = List(protoFixture, avroFixture, avroWithSchemaFixture, avroRMFixture)

  property(
    "RPC Server should " +
      "be able to serialize and deserialize BigDecimal in a Request using proto format"
  ) {
    import RPCService._
    val client = protoFixture()
    forAll { (bd: BigDecimal, s: String) =>
      client.bigDecimalProtoWrapper(Request(bd, s)).unsafeRunSync() == Response(
        bd,
        s,
        check = true
      )
    }
  }

  property(
    "RPC Server should " +
      "be able to serialize and deserialize BigDecimal using avro format"
  ) {

    val client = avroFixture()
    forAll(bigDecimalGen(2)) { bd =>
      client.bigDecimalAvro(tag[(Nat._8, Nat._2)][BigDecimal](bd)).unsafeRunSync() == bd
    }
  }

  property(
    "RPC Server should " +
      "be able to serialize and deserialize BigDecimal in a Request using avro format"
  ) {

    import RPCService._
    val client = avroFixture()
    forAll { request: RequestPS =>
      client.bigDecimalAvroWrapper(request).unsafeRunSync() == ResponsePS(
        request.bd1,
        request.bd2,
        request.bd3,
        request.label,
        check = true
      )
    }
  }

  property(
    "RPC Server should " +
      "be able to serialize and deserialize BigDecimal using avro with schema format"
  ) {

    val client = avroWithSchemaFixture()
    forAll(bigDecimalGen(2)) { bd =>
      client
        .bigDecimalAvroWithSchema(tag[(Nat._8, Nat._2)][BigDecimal](bd))
        .unsafeRunSync() == bd
    }
  }

  property(
    "RPC Server should " +
      "be able to serialize and deserialize BigDecimal in a Request using avro with schema format"
  ) {

    import RPCService._
    val client = avroWithSchemaFixture()
    forAll { request: RequestPS =>
      client
        .bigDecimalAvroWithSchemaWrapper(request)
        .unsafeRunSync() == ResponsePS(
        request.bd1,
        request.bd2,
        request.bd3,
        request.label,
        check = true
      )
    }
  }

  property(
    "A RPC server with an implicit rounding mode should " +
      "be able to serialize and deserialize BigDecimal using avro format"
  ) {

    import RPCServiceWithImplicitRM._
    val client = avroRMFixture()
    forAll(bigDecimalGen(12)) { bd =>
      client
        .bigDecimalAvro(tag[(Nat._8, Nat._2)][BigDecimal](bd))
        .unsafeRunSync() == bd
        .setScale(2, RM)
    }
  }

  property(
    "A RPC server with an implicit rounding mode should " +
      "be able to serialize and deserialize BigDecimal in a Request using avro"
  ) {

    import RPCServiceWithImplicitRM._
    val client = avroRMFixture()
    forAll(bigDecimalGen(12)) { bd =>
      val bdTagged = tag[(Nat._8, Nat._2)][BigDecimal](bd)
      client
        .bigDecimalAvroWrapper(Request(bdTagged, "label"))
        .unsafeRunSync() == Response(
        tag[(Nat._8, Nat._2)][BigDecimal](bd.setScale(2, RM)),
        "label",
        check = true
      )
    }
  }
}
