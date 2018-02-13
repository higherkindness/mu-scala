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

object Utils extends CommonUtils {

  object service {

    @service
    trait RPCService[F[_]] {

      @rpc(Avro) def unary(a: A): F[C]

      @rpc(Avro, Gzip) def unaryCompressed(a: A): F[C]

      @rpc(Protobuf)
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): Stream[F, C]

      @rpc(Protobuf, Gzip)
      @stream[ResponseStreaming.type]
      def serverStreamingCompressed(b: B): Stream[F, C]

      @rpc(Protobuf)
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Stream[F, A]): F[D]

      @rpc(Protobuf, Gzip)
      @stream[RequestStreaming.type]
      def clientStreamingCompressed(oa: Stream[F, A]): F[D]

      @rpc(Avro)
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Stream[F, E]): Stream[F, E]

      @rpc(Avro, Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressed(oe: Stream[F, E]): Stream[F, E]

    }

  }

  object handlers {

    object server {

      import database._
      import service._

      class ServerRPCService[F[_]: Effect] extends RPCService[F] {

        def unary(a: A): F[C] = Effect[F].delay(c1)

        def unaryCompressed(a: A): F[C] = unary(a)

        def serverStreaming(b: B): Stream[F, C] = {
          debug(s"[fs2 - SERVER] b -> $b")
          Stream.fromIterator(cList.iterator)
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

        def biStreamingCompressed(oe: Stream[F, E]): Stream[F, E] = biStreaming(oe)

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
      AddService(RPCService.bindService[ConcurrentMonad])
    )

    implicit val serverW: ServerW = createServerConf(grpcConfigs)

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCServiceClient: RPCService.Client[ConcurrentMonad] =
      RPCService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
