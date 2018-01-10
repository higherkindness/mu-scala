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

import cats.{~>, Applicative, Monad, MonadError}
import freestyle.tagless._
import freestyle.rpc.common._
import freestyle.free.asyncCatsEffect.implicits._
import freestyle.rpc.protocol._
import monix.eval.Task
import monix.reactive.Observable

object Utils extends CommonUtils {

  object service {

    @service
    trait RPCService[F[_]] {

      import ExternalScope._

      @rpc(Protobuf) def notAllowed(b: Boolean): F[C]

      @rpc(Avro) def unary(a: A): F[C]

      @rpc(Protobuf) def empty(empty: Empty.type): F[Empty.type]

      @rpc(Protobuf) def emptyParam(a: A): F[Empty.type]

      @rpc(Protobuf) def emptyParamResponse(empty: Empty.type): F[A]

      @rpc(Avro) def emptyAvro(empty: Empty.type): F[Empty.type]

      @rpc(Avro) def emptyAvroParam(a: A): F[Empty.type]

      @rpc(Avro) def emptyAvroParamResponse(empty: Empty.type): F[A]

      @rpc(Protobuf)
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): F[Observable[C]]

      @rpc(Protobuf)
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Observable[A]): F[D]

      @rpc(Avro)
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Observable[E]): F[Observable[E]]

      @rpc(Protobuf)
      def scope(empty: Empty.type): F[External]
    }

  }

  object handlers {

    object server {

      import database._
      import service._
      import freestyle.rpc.protocol._

      class ServerRPCService[F[_]](implicit F: Applicative[F], T2F: Task ~> F)
          extends RPCService[F] {

        def notAllowed(b: Boolean): F[C] = F.pure(c1)

        def empty(empty: Empty.type): F[Empty.type] = F.pure(Empty)

        def emptyParam(a: A): F[Empty.type] = F.pure(Empty)

        def emptyParamResponse(empty: Empty.type): F[A] = F.pure(a4)

        def emptyAvro(empty: Empty.type): F[Empty.type] = F.pure(Empty)

        def emptyAvroParam(a: A): F[Empty.type] = F.pure(Empty)

        def emptyAvroParamResponse(empty: Empty.type): F[A] = F.pure(a4)

        def unary(a: A): F[C] =
          F.pure(c1)

        def serverStreaming(b: B): F[Observable[C]] = {
          debug(s"[SERVER] b -> $b")
          val obs = Observable.fromIterable(cList)
          F.pure(obs)
        }

        def clientStreaming(oa: Observable[A]): F[D] =
          T2F(
            oa.foldLeftL(D(0)) {
              case (current, a) =>
                debug(s"[SERVER] Current -> $current / a -> $a")
                D(current.bar + a.x + a.y)
            }
          )

        def biStreaming(oe: Observable[E]): F[Observable[E]] =
          F.pure {
            oe.flatMap { e: E =>
              save(e)

              Observable.fromIterable(eList)
            }
          }

        def save(e: E) = e // do something else with e?

        import ExternalScope._

        override def scope(empty: protocol.Empty.type): F[External] = F.pure(External(e1))
      }

    }

    object client {

      import freestyle.rpc.Utils.clientProgram.MyRPCClient
      import service._
      import freestyle.rpc.protocol._

      class FreesRPCServiceClientHandler[F[_]: Monad](
          implicit client: RPCService.Client[F],
          M: MonadError[F, Throwable],
          T2F: Task ~> F)
          extends MyRPCClient.Handler[F] {

        override protected[this] def notAllowed(b: Boolean): F[C] =
          client.notAllowed(b)

        override protected[this] def empty: F[Empty.type] =
          client.empty(protocol.Empty)

        override protected[this] def emptyParam(a: A): F[Empty.type] =
          client.emptyParam(a)

        override protected[this] def emptyParamResponse: F[A] =
          client.emptyParamResponse(protocol.Empty)

        override protected[this] def emptyAvro: F[Empty.type] =
          client.emptyAvro(protocol.Empty)

        override protected[this] def emptyAvroParam(a: A): F[Empty.type] =
          client.emptyAvroParam(a)

        override protected[this] def emptyAvroParamResponse: F[A] =
          client.emptyAvroParamResponse(protocol.Empty)

        override protected[this] def u(x: Int, y: Int): F[C] =
          client.unary(A(x, y))

        override protected[this] def ss(a: Int, b: Int): F[List[C]] = T2F {
          client
            .serverStreaming(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
        }

        override protected[this] def cs(cList: List[C], bar: Int): F[D] =
          client.clientStreaming(Observable.fromIterable(cList.map(c => c.a)))

        override protected[this] def bs(eList: List[E]): F[E] =
          T2F(
            client
              .biStreaming(Observable.fromIterable(eList))
              .firstL)

      }

    }

  }

  trait FreesRuntime extends CommonRuntime {

    import service._
    import handlers.server._
    import handlers.client._
    import freestyle.rpc.server._
    import freestyle.rpc.server.implicits._

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
      RPCService.client[ConcurrentMonad](createManagedChannelFor)

    implicit val freesRPCServiceClientHandler: FreesRPCServiceClientHandler[ConcurrentMonad] =
      new FreesRPCServiceClientHandler[ConcurrentMonad]

  }

  object implicits extends FreesRuntime

}
