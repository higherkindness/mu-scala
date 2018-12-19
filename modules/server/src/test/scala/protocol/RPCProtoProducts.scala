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

package higherkindness.mu.rpc
package protocol

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.protocol.Utils.withClient
import higherkindness.mu.rpc.testing.servers.withServerChannel
import org.scalatest._
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers

class RPCProtoProducts extends RpcBaseTestSuite with BeforeAndAfterAll with Checkers {

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

  "A RPC server" should {

    import TestsImplicits._
    import RPCService._

    implicit val H: RPCServiceDefImpl[ConcurrentMonad] =
      new RPCServiceDefImpl[ConcurrentMonad]

    "be able to serialize and deserialize Options in the request/response using proto format" in {

      withServerChannel(ProtoRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(ProtoRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) {
          client =>
            check {
              forAll { maybeString: Option[String] =>
                client
                  .optionProto(RequestOption(maybeString.map(MyParam)))
                  .unsafeRunSync() == ResponseOption(maybeString, true)
              }
            }
        }

      }

    }

    "be able to serialize and deserialize Options in the request/response using avro format" in {

      withServerChannel(AvroRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(AvroRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) { client =>
          check {
            forAll { maybeString: Option[String] =>
              client
                .optionAvro(RequestOption(maybeString.map(MyParam)))
                .unsafeRunSync() == ResponseOption(maybeString, true)
            }
          }
        }
      }

    }

    "be able to serialize and deserialize Options in the request/response using avro with schema format" in {

      withServerChannel(AvroWithSchemaRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(AvroWithSchemaRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) {
          client =>
            check {
              forAll { maybeString: Option[String] =>
                client
                  .optionAvroWithSchema(RequestOption(maybeString.map(MyParam)))
                  .unsafeRunSync() == ResponseOption(maybeString, true)
              }
            }
        }
      }

    }

    "be able to serialize and deserialize Lists in the request/response using proto format" in {

      withServerChannel(ProtoRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(ProtoRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) {
          client =>
            check {
              forAll { list: List[String] =>
                client
                  .listProto(RequestList(list.map(MyParam)))
                  .unsafeRunSync() == ResponseList(list, true)
              }
            }
        }
      }

    }

    "be able to serialize and deserialize Lists in the request/response using avro format" in {

      withServerChannel(AvroRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(AvroRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) { client =>
          check {
            forAll { list: List[String] =>
              client
                .listAvro(RequestList(list.map(MyParam)))
                .unsafeRunSync() == ResponseList(list, true)
            }
          }
        }
      }

    }

    "be able to serialize and deserialize Lists in the request/response using avro with schema format" in {

      withServerChannel(AvroWithSchemaRPCServiceDef.bindService[ConcurrentMonad]) { sc =>
        withClient(AvroWithSchemaRPCServiceDef.clientFromChannel[ConcurrentMonad](IO(sc.channel))) {
          client =>
            check {
              forAll { list: List[String] =>
                client
                  .listAvroWithSchema(RequestList(list.map(MyParam)))
                  .unsafeRunSync() == ResponseList(list, true)
              }
            }
        }
      }

    }
  }

}
