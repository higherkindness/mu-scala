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
package protocol

import org.joda.time._
import cats.Applicative
import cats.effect.{IO, Resource}
import cats.syntax.applicative._
import com.fortysevendeg.scalacheck.datetime.instances.joda._
import com.fortysevendeg.scalacheck.datetime.GenDateTime._
import mu.rpc.common._
import mu.rpc.testing.servers.{withServerChannel, ServerChannel}
import org.scalacheck.Arbitrary
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class RPCJodaLocalDateTimeTests extends RpcBaseTestSuite with BeforeAndAfter with Checkers {

  def withClient[Client, A](resource: Resource[ConcurrentMonad, Client])(f: Client => A): A =
    resource.use(client => IO(f(client))).unsafeRunSync()

  object RPCJodaLocalDateTimeService {

    case class Request(date: LocalDateTime, label: String)

    case class Response(date: LocalDateTime, label: String, check: Boolean)

    object ProtobufService {
      import mu.rpc.marshallers.jodaTimeEncoders.pbd._
      @service(Protobuf)
      trait Def[F[_]] {
        def jodaLocalDateTimeProto(date: LocalDateTime): F[LocalDateTime]
        def jodaLocalDateTimeReqProto(request: Request): F[Response]
      }
    }

    object AvroService {
      import mu.rpc.marshallers.jodaTimeEncoders.avro._
      import mu.rpc.marshallers.jodaTimeEncoders.avro.marshallers._
      @service(Avro)
      trait Def[F[_]] {
        def jodaLocalDateTimeAvro(date: LocalDateTime): F[LocalDateTime]
        def jodaLocalDateTimeReqAvro(request: Request): F[Response]
      }
      @service(AvroWithSchema)
      trait WithSchemaDef[F[_]] {
        def localDateTimeAvroWithSchema(date: LocalDateTime): F[LocalDateTime]
        def jodaLocalDateTimeReqAvroWithSchema(request: Request): F[Response]
      }
    }

    class RPCJodaServiceDefHandler[F[_]: Applicative]
        extends ProtobufService.Def[F]
        with AvroService.Def[F]
        with AvroService.WithSchemaDef[F] {

      def jodaLocalDateTimeProto(date: LocalDateTime): F[LocalDateTime] = date.pure
      def jodaLocalDateTimeReqProto(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure

      def jodaLocalDateTimeAvro(date: LocalDateTime): F[LocalDateTime] = date.pure
      def jodaLocalDateTimeReqAvro(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure

      def localDateTimeAvroWithSchema(date: LocalDateTime): F[LocalDateTime] = date.pure
      override def jodaLocalDateTimeReqAvroWithSchema(request: Request): F[Response] =
        Response(request.date, request.label, check = true).pure

    }

  }

  "RPCJodaService" should {

    import TestsImplicits._
    import RPCJodaLocalDateTimeService._

    def protoClient(
        sc: ServerChannel): Resource[ConcurrentMonad, ProtobufService.Def[ConcurrentMonad]] =
      ProtobufService.Def
        .clientFromChannel[ConcurrentMonad, ProtobufService.Def[ConcurrentMonad]](
          suspendM(sc.channel))

    def avroClient(sc: ServerChannel): Resource[ConcurrentMonad, AvroService.Def[ConcurrentMonad]] =
      AvroService.Def
        .clientFromChannel[ConcurrentMonad, AvroService.Def[ConcurrentMonad]](suspendM(sc.channel))

    def avroWSClient(
        sc: ServerChannel): Resource[ConcurrentMonad, AvroService.WithSchemaDef[ConcurrentMonad]] =
      AvroService.WithSchemaDef
        .clientFromChannel[ConcurrentMonad, AvroService.WithSchemaDef[ConcurrentMonad]](
          suspendM(sc.channel))

    implicit val H: RPCJodaServiceDefHandler[ConcurrentMonad] =
      new RPCJodaServiceDefHandler[ConcurrentMonad]

    val from: DateTime = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    val range: Period  = Period.years(200)

    "be able to serialize and deserialize joda.time.LocalDateTime using proto format" in {

      withServerChannel(ProtobufService.Def.bindService[ConcurrentMonad]) { sc =>
        withClient(protoClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
              val date = dt.toLocalDateTime
              client.jodaLocalDateTimeProto(date).unsafeRunSync() == date

            }
          }
        }
      }
    }

    "be able to serialize and deserialize joda.LocalDateTime in a Request using proto format" in {

      withServerChannel(ProtobufService.Def.bindService[ConcurrentMonad]) { sc =>
        withClient(protoClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
              (dt: DateTime, s: String) =>
                val date = dt.toLocalDateTime
                client
                  .jodaLocalDateTimeReqProto(Request(date, s))
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

    "be able to serialize and deserialize joda.LocalDateTime using avro format" in {
      withServerChannel(AvroService.Def.bindService[ConcurrentMonad]) { sc =>
        withClient(avroClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
              val date = dt.toLocalDateTime
              client.jodaLocalDateTimeAvro(date).unsafeRunSync() == date
            }
          }
        }
      }
    }

    "be able to serialize and deserialize joda.LocalDateTime in a Request using avro format" in {

      withServerChannel(AvroService.Def.bindService[ConcurrentMonad]) { sc =>
        withClient(avroClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
              (dt: DateTime, s: String) =>
                val date = dt.toLocalDateTime
                client
                  .jodaLocalDateTimeReqAvro(Request(date, s))
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

    "be able to serialize and deserialize joda.LocalDateTime using avro with schema format" in {
      withServerChannel(AvroService.WithSchemaDef.bindService[ConcurrentMonad]) { sc =>
        withClient(avroWSClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range)) { dt: DateTime =>
              val date = dt.toLocalDateTime
              client.localDateTimeAvroWithSchema(date).unsafeRunSync() == date
            }
          }
        }
      }
    }

    "be able to serialize and deserialize joda.LocalDateTime in a Request using avro with schema format" in {

      withServerChannel(AvroService.WithSchemaDef.bindService[ConcurrentMonad]) { sc =>
        withClient(avroWSClient(sc)) { client =>
          check {
            forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
              (dt: DateTime, s: String) =>
                val date = dt.toLocalDateTime
                client
                  .jodaLocalDateTimeReqAvroWithSchema(Request(date, s))
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

}
