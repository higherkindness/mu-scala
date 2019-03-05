/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

import org.joda.time._
import cats.Applicative
import cats.effect.Resource
import cats.syntax.applicative._
import com.fortysevendeg.scalacheck.datetime.instances.joda._
import com.fortysevendeg.scalacheck.datetime.GenDateTime._
import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.testing.servers.withServerChannel
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import org.scalacheck.Arbitrary
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatestplus.scalacheck.Checkers

class RPCJodaLocalDateTests extends RpcBaseTestSuite with BeforeAndAfterAll with Checkers {

  def withClient[Client, A](
      serviceDef: ConcurrentMonad[ServerServiceDefinition],
      resourceBuilder: ConcurrentMonad[ManagedChannel] => Resource[ConcurrentMonad, Client])(
      f: Client => A): A =
    withServerChannel(serviceDef)
      .flatMap(sc => resourceBuilder(suspendM(sc.channel)))
      .use(client => suspendM(f(client)))
      .unsafeRunSync()

  object RPCJodaLocalDateService {

    case class Request(date: LocalDate, label: String)

    case class Response(date: LocalDate, label: String, check: Boolean)

    object ProtobufService {
      import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.pbd._
      @service(Protobuf)
      trait Def[F[_]] {
        def jodaLocalDateProto(date: LocalDate): F[LocalDate]
        def jodaLocalDateReqProto(request: Request): F[Response]
      }
    }

    object AvroService {
      import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro._
      import higherkindness.mu.rpc.marshallers.jodaTimeEncoders.avro.marshallers._
      @service(Avro)
      trait Def[F[_]] {
        def jodaLocalDateAvro(date: LocalDate): F[LocalDate]
        def jodaLocalDateReqAvro(request: Request): F[Response]
      }
      @service(AvroWithSchema)
      trait WithSchemaDef[F[_]] {
        def localDateAvroWithSchema(date: LocalDate): F[LocalDate]
        def jodaLocalDateReqAvroWithSchema(request: Request): F[Response]
      }
    }

    class RPCLocalDateServiceDefHandler[F[_]: Applicative]
        extends ProtobufService.Def[F]
        with AvroService.Def[F]
        with AvroService.WithSchemaDef[F] {

      def jodaLocalDateProto(date: LocalDate): F[LocalDate] = date.pure
      def jodaLocalDateReqProto(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure

      def jodaLocalDateAvro(date: LocalDate): F[LocalDate] = date.pure
      def jodaLocalDateReqAvro(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure

      def localDateAvroWithSchema(date: LocalDate): F[LocalDate] = date.pure
      override def jodaLocalDateReqAvroWithSchema(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure
    }

  }

  "RPCJodaService" should {

    import TestsImplicits._
    import RPCJodaLocalDateService._

    implicit val H: RPCLocalDateServiceDefHandler[ConcurrentMonad] =
      new RPCLocalDateServiceDefHandler[ConcurrentMonad]

    val from: DateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    val range: Period  = Period.years(200)

    "be able to serialize and deserialize joda.time.LocalDate using proto format" in {

      withClient(
        ProtobufService.Def.bindService[ConcurrentMonad],
        ProtobufService.Def.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
            val date = dt.toLocalDate
            client.jodaLocalDateProto(date).unsafeRunSync() == date

          }
        }

      }
    }

    "be able to serialize and deserialize joda.time.LocalDate in a Request using proto format" in {

      withClient(
        ProtobufService.Def.bindService[ConcurrentMonad],
        ProtobufService.Def.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (dt: DateTime, s: String) =>
              val date = dt.toLocalDate
              client.jodaLocalDateReqProto(Request(date, s)).unsafeRunSync() == Response(
                date,
                s,
                check = true
              )
          }
        }

      }
    }

    "be able to serialize and deserialize joda.time.LocalDate using avro format" in {

      withClient(
        AvroService.Def.bindService[ConcurrentMonad],
        AvroService.Def.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
            val date = dt.toLocalDate
            client.jodaLocalDateAvro(date).unsafeRunSync() == date

          }
        }

      }
    }

    "be able to serialize and deserialize joda.time.LocalDate in a Request using avro format" in {

      withClient(
        AvroService.Def.bindService[ConcurrentMonad],
        AvroService.Def.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (dt: DateTime, s: String) =>
              val date = dt.toLocalDate
              client.jodaLocalDateReqAvro(Request(date, s)).unsafeRunSync() == Response(
                date,
                s,
                check = true
              )
          }
        }

      }
    }

    "be able to serialize and deserialize joda.time.LocalDate using avro with schema format" in {

      withClient(
        AvroService.WithSchemaDef.bindService[ConcurrentMonad],
        AvroService.WithSchemaDef.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
            val date = dt.toLocalDate
            client.localDateAvroWithSchema(date).unsafeRunSync() == date
          }
        }

      }
    }

    "be able to serialize and deserialize joda.time.LocalDate in a Request using avro with schema format" in {

      withClient(
        AvroService.WithSchemaDef.bindService[ConcurrentMonad],
        AvroService.WithSchemaDef.clientFromChannel[ConcurrentMonad](_)) { client =>
        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (dt: DateTime, s: String) =>
              val date = dt.toLocalDate
              client
                .jodaLocalDateReqAvroWithSchema(Request(date, s))
                .unsafeRunSync() == Response(
                date,
                s,
                check = true
              )
          }
        }

      }
    }

  }
}
