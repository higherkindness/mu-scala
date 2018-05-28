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
package fs2

import freestyle.rpc.common._
import freestyle.rpc.protocol._
import _root_.fs2._
import cats.effect.Effect
import io.grpc.Status

object Utils extends CommonUtils {

  object service {

    @service(Avro)
    trait RPCAvroService[F[_]] {

      @rpc def unary(a: A): F[C]

      @rpc(Gzip) def unaryCompressed(a: A): F[C]

      @rpc
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Stream[F, E]): Stream[F, E]

      @rpc(Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressed(oe: Stream[F, E]): Stream[F, E]

    }

    object RPCAvroService {
      // this companion object is here to make sure @service supports
      // companion objects
    }

    @service(AvroWithSchema)
    trait RPCAvroWithSchemaService[F[_]] {

      @rpc def unaryWithSchema(a: A): F[C]

      @rpc(Gzip) def unaryCompressedWithSchema(a: A): F[C]

      @rpc
      @stream[BidirectionalStreaming.type]
      def biStreamingWithSchema(oe: Stream[F, E]): Stream[F, E]

      @rpc(Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressedWithSchema(oe: Stream[F, E]): Stream[F, E]

    }

    object RPCAvroWithSchemaService {
      // this companion object is here to make sure @service supports
      // companion objects
    }

    @service(Protobuf)
    trait RPCProtobufService[F[_]] {

      @rpc
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): Stream[F, C]

      @rpc
      @stream[ResponseStreaming.type]
      def serverStreamingWithError(e: E): Stream[F, C]

      @rpc(Gzip)
      @stream[ResponseStreaming.type]
      def serverStreamingCompressed(b: B): Stream[F, C]

      @rpc
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Stream[F, A]): F[D]

      @rpc(Gzip)
      @stream[RequestStreaming.type]
      def clientStreamingCompressed(oa: Stream[F, A]): F[D]

    }

    object RPCProtobufService {
      // this companion object is here to make sure @service supports
      // companion objects
    }

  }

  object handlers {

    object server {

      import database._
      import service._

      class ServerRPCService[F[_]: Effect]
          extends RPCAvroService[F]
          with RPCAvroWithSchemaService[F]
          with RPCProtobufService[F] {

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

  trait FreesRuntime {

    import service._
    import handlers.server._
    import freestyle.rpc.server._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCHandler: ServerRPCService[ConcurrentMonad] =
      new ServerRPCService[ConcurrentMonad]

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(RPCAvroService.bindService[ConcurrentMonad]),
      AddService(RPCAvroWithSchemaService.bindService[ConcurrentMonad]),
      AddService(RPCProtobufService.bindService[ConcurrentMonad])
    )

    implicit val serverW: ServerW = createServerConf(grpcConfigs)

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val avroClient: RPCAvroService.Client[ConcurrentMonad] =
      RPCAvroService.client[ConcurrentMonad](createChannelFor)

    implicit val avroWithSchemaClient: RPCAvroWithSchemaService.Client[ConcurrentMonad] =
      RPCAvroWithSchemaService.client[ConcurrentMonad](createChannelFor)

    implicit val protobufClient: RPCProtobufService.Client[ConcurrentMonad] =
      RPCProtobufService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
