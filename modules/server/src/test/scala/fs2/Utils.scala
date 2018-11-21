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
package fs2

import mu.rpc.common._
import mu.rpc.protocol._
import _root_.fs2._
import cats.effect.Effect
import io.grpc.Status

object Utils extends CommonUtils {

  object service {

    @service(Protobuf) trait ProtoRPCService[F[_]] {
      def serverStreamingWithError(e: E): Stream[F, C]
      def clientStreaming(oa: Stream[F, A]): F[D]
      def serverStreaming(b: B): Stream[F, C]
    }

    @service(Protobuf, Gzip) trait CompressedProtoRPCService[F[_]] {
      def serverStreamingCompressed(b: B): Stream[F, C]
      def clientStreamingCompressed(oa: Stream[F, A]): F[D]
    }

    @service(Avro) trait AvroRPCService[F[_]] {
      def unary(a: A): F[C]
      def biStreaming(oe: Stream[F, E]): Stream[F, E]
    }

    @service(Avro, Gzip) trait CompressedAvroRPCService[F[_]] {
      def unaryCompressed(a: A): F[C]
      def biStreamingCompressed(oe: Stream[F, E]): Stream[F, E]
    }

    @service(AvroWithSchema) trait AvroWithSchemaRPCService[F[_]] {
      def unaryWithSchema(a: A): F[C]
      def biStreamingWithSchema(oe: Stream[F, E]): Stream[F, E]
    }

    @service(AvroWithSchema, Gzip) trait CompressedAvroWithSchemaRPCService[F[_]] {
      def unaryCompressedWithSchema(a: A): F[C]
      def biStreamingCompressedWithSchema(oe: Stream[F, E]): Stream[F, E]
    }

    // this companion objects are here to make sure @service supports
    // companion objects
    object ProtoRPCService          {}
    object AvroRPCService           {}
    object AvroWithSchemaRPCService {}

  }

  object handlers {

    object server {

      import database._
      import service._

      class ServerRPCService[F[_]: Effect]
          extends ProtoRPCService[F]
          with AvroRPCService[F]
          with AvroWithSchemaRPCService[F]
          with CompressedProtoRPCService[F]
          with CompressedAvroRPCService[F]
          with CompressedAvroWithSchemaRPCService[F] {

        def unary(a: A): F[C] = Effect[F].delay(c1)

        def unaryWithSchema(a: A): F[C] = unary(a)

        def unaryCompressed(a: A): F[C] = unary(a)

        def unaryCompressedWithSchema(a: A): F[C] = unaryCompressed(a)

        def serverStreaming(b: B): Stream[F, C] = {
          debug(s"[fs2 - SERVER] b -> $b")
          Stream.fromIterator(cList.iterator)
        }

        def serverStreamingWithError(e: E): Stream[F, C] = e.foo match {
          case "SE" =>
            Stream.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asException)
          case "SRE" =>
            Stream.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asRuntimeException)
          case "RTE" =>
            Stream.raiseError(new IllegalArgumentException(e.foo))
          case _ =>
            sys.error(e.foo)
        }

        def serverStreamingCompressed(b: B): Stream[F, C] = serverStreaming(b)

        def clientStreaming(oa: Stream[F, A]): F[D] =
          oa.compile.fold(D(0)) {
            case (current, a) =>
              debug(s"[fs2 - SERVER] Current -> $current / a -> $a")
              D(current.bar + a.x + a.y)
          }

        def clientStreamingCompressed(oa: Stream[F, A]): F[D] = clientStreaming(oa)

        def biStreaming(oe: Stream[F, E]): Stream[F, E] =
          oe.flatMap { e: E =>
            save(e)
            Stream.fromIterator(eList.iterator)
          }

        def biStreamingWithSchema(oe: Stream[F, E]): Stream[F, E] = biStreaming(oe)

        def biStreamingCompressed(oe: Stream[F, E]): Stream[F, E] = biStreaming(oe)

        def biStreamingCompressedWithSchema(oe: Stream[F, E]): Stream[F, E] =
          biStreamingCompressed(oe)

        def save(e: E): E = e // do something else with e?

      }

    }

  }

  trait MuRuntime {

    import TestsImplicits._
    import service._
    import handlers.server._
    import mu.rpc.server._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val muRPCHandler: ServerRPCService[ConcurrentMonad] =
      new ServerRPCService[ConcurrentMonad]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(ProtoRPCService.bindService[ConcurrentMonad]),
      AddService(AvroRPCService.bindService[ConcurrentMonad]),
      AddService(AvroWithSchemaRPCService.bindService[ConcurrentMonad]),
      AddService(CompressedProtoRPCService.bindService[ConcurrentMonad]),
      AddService(CompressedAvroRPCService.bindService[ConcurrentMonad]),
      AddService(CompressedAvroWithSchemaRPCService.bindService[ConcurrentMonad])
    )

    implicit val grpcServer: GrpcServer[ConcurrentMonad] =
      createServerConf[ConcurrentMonad](grpcConfigs).unsafeRunSync

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val muProtoRPCServiceClient: ProtoRPCService.Client[ConcurrentMonad] =
      ProtoRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val muAvroRPCServiceClient: AvroRPCService.Client[ConcurrentMonad] =
      AvroRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val muAvroWithSchemaRPCServiceClient: AvroWithSchemaRPCService.Client[
      ConcurrentMonad] =
      AvroWithSchemaRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val muCompressedProtoRPCServiceClient: CompressedProtoRPCService.Client[
      ConcurrentMonad] =
      CompressedProtoRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val muCompressedAvroRPCServiceClient: CompressedAvroRPCService.Client[
      ConcurrentMonad] =
      CompressedAvroRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val muCompressedAvroWithSchemaRPCServiceClient: CompressedAvroWithSchemaRPCService.Client[
      ConcurrentMonad] =
      CompressedAvroWithSchemaRPCService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends MuRuntime

}
