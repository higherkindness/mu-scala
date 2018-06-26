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

package freestyle.rpc
package protocol

import java.time._

import cats.Applicative
import cats.syntax.applicative._
import com.fortysevendeg.scalacheck.datetime.instances.jdk8._
import com.fortysevendeg.scalacheck.datetime.GenDateTime._
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.seconds
import freestyle.rpc.common._
import freestyle.rpc.testing.servers.withServerChannel
import org.scalacheck.Arbitrary
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class RPCJavaTimeTests extends RpcBaseTestSuite with BeforeAndAfterAll with Checkers {

  object RPCDateService {

    case class Request(date: LocalDate, dateTime: LocalDateTime, label: String)

    case class Response(date: LocalDate, dateTime: LocalDateTime, result: String, check: Boolean)

    @service(Protobuf)
    trait ProtoRPCDateServiceDef[F[_]] {
      @rpc def localDateProto(date: LocalDate): F[LocalDate]
      @rpc def localDateTimeProto(dateTime: LocalDateTime): F[LocalDateTime]
      @rpc def dateProtoWrapper(req: Request): F[Response]
    }

    @service(Avro)
    trait AvroRPCDateServiceDef[F[_]] {
      @rpc def localDateAvro(date: LocalDate): F[LocalDate]
      @rpc def localDateTimeAvro(dateTime: LocalDateTime): F[LocalDateTime]
      @rpc def dateAvroWrapper(req: Request): F[Response]
    }

    @service(AvroWithSchema)
    trait AvroWithSchemaRPCDateServiceDef[F[_]] {
      @rpc def localDateAvroWithSchema(date: LocalDate): F[LocalDate]
      @rpc def localDateTimeAvroWithSchema(dateTime: LocalDateTime): F[LocalDateTime]
      @rpc def dateAvroWrapperWithSchema(req: Request): F[Response]
    }

    class RPCDateServiceDefImpl[F[_]: Applicative]
        extends ProtoRPCDateServiceDef[F]
        with AvroRPCDateServiceDef[F]
        with AvroWithSchemaRPCDateServiceDef[F] {

      def localDateProto(date: LocalDate): F[LocalDate]                 = date.pure
      def localDateTimeProto(dateTime: LocalDateTime): F[LocalDateTime] = dateTime.pure
      def dateProtoWrapper(req: Request): F[Response] =
        Response(req.date, req.dateTime, req.label, check = true).pure
      def localDateAvro(date: LocalDate): F[LocalDate]                 = date.pure
      def localDateTimeAvro(dateTime: LocalDateTime): F[LocalDateTime] = dateTime.pure
      def dateAvroWrapper(req: Request): F[Response] =
        Response(req.date, req.dateTime, req.label, check = true).pure
      def localDateAvroWithSchema(date: LocalDate): F[LocalDate]                 = date.pure
      def localDateTimeAvroWithSchema(dateTime: LocalDateTime): F[LocalDateTime] = dateTime.pure
      def dateAvroWrapperWithSchema(req: Request): F[Response] =
        Response(req.date, req.dateTime, req.label, check = true).pure
    }

  }

  "A RPC server" should {

    import RPCDateService._
    import monix.execution.Scheduler.Implicits.global

    implicit val H: RPCDateServiceDefImpl[ConcurrentMonad] =
      new RPCDateServiceDefImpl[ConcurrentMonad]

    val from: ZonedDateTime = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val range: Duration     = Duration.ofDays(365 * 200)

    "be able to serialize and deserialize LocalDate using proto format" in {

      withServerChannel(ProtoRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: ProtoRPCDateServiceDef.Client[ConcurrentMonad] =
          ProtoRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val date = zdt.toLocalDate
            client.localDateProto(date).unsafeRunSync() == date
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDateTime using proto format" in {

      withServerChannel(ProtoRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: ProtoRPCDateServiceDef.Client[ConcurrentMonad] =
          ProtoRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val dateTime = zdt.toLocalDateTime
            client.localDateTimeProto(dateTime).unsafeRunSync() == dateTime
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDate and LocalDateTime in a Request using proto format" in {

      withServerChannel(ProtoRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: ProtoRPCDateServiceDef.Client[ConcurrentMonad] =
          ProtoRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (zdt: ZonedDateTime, s: String) =>
              val date     = zdt.toLocalDate
              val dateTime = zdt.toLocalDateTime
              client.dateProtoWrapper(Request(date, dateTime, s)).unsafeRunSync() == Response(
                date,
                dateTime,
                s,
                check = true)
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDate using avro format" in {

      withServerChannel(AvroRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val date = zdt.toLocalDate
            client.localDateAvro(date).unsafeRunSync() == date
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDateTime using avro format" in {

      withServerChannel(AvroRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val dateTime = zdt.toLocalDateTime
            client.localDateTimeAvro(dateTime).unsafeRunSync() == dateTime
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDate and LocalDateTime in a Request using avro format" in {

      withServerChannel(AvroRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (zdt: ZonedDateTime, s: String) =>
              val date     = zdt.toLocalDate
              val dateTime = zdt.toLocalDateTime
              client.dateAvroWrapper(Request(date, dateTime, s)).unsafeRunSync() == Response(
                date,
                dateTime,
                s,
                check = true)
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDate using avro format with schema" in {

      withServerChannel(AvroWithSchemaRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroWithSchemaRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroWithSchemaRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val date = zdt.toLocalDate
            client.localDateAvroWithSchema(date).unsafeRunSync() == date
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDateTime using avro format with schema" in {

      withServerChannel(AvroWithSchemaRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroWithSchemaRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroWithSchemaRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range)) { zdt: ZonedDateTime =>
            val dateTime = zdt.toLocalDateTime
            client.localDateTimeAvroWithSchema(dateTime).unsafeRunSync() == dateTime
          }
        }

      }

    }

    "be able to serialize and deserialize LocalDate and LocalDateTime in a Request using avro format with schema" in {

      withServerChannel(AvroWithSchemaRPCDateServiceDef.bindService[ConcurrentMonad]) { sc =>
        val client: AvroWithSchemaRPCDateServiceDef.Client[ConcurrentMonad] =
          AvroWithSchemaRPCDateServiceDef.clientFromChannel[ConcurrentMonad](sc.channel)

        check {
          forAll(genDateTimeWithinRange(from, range), Arbitrary.arbitrary[String]) {
            (zdt: ZonedDateTime, s: String) =>
              val date     = zdt.toLocalDate
              val dateTime = zdt.toLocalDateTime
              client
                .dateAvroWrapperWithSchema(Request(date, dateTime, s))
                .unsafeRunSync() == Response(date, dateTime, s, check = true)
          }
        }

      }

    }

  }

}
