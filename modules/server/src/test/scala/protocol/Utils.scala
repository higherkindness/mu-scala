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

import cats.MonadError
import cats.effect.Async
import cats.syntax.applicative._
import freestyle.rpc.common._
import freestyle.rpc.server.implicits._
import freestyle.tagless.tagless
import monix.reactive.Observable

object Utils extends CommonUtils {

  object service {

    @service
    trait RPCService[F[_]] {

      import ExternalScope._

      @rpc(Protobuf) def notAllowed(b: Boolean): F[C]

      @rpc(Protobuf, Gzip) def notAllowedCompressed(b: Boolean): F[C]

      @rpc(Avro) def unary(a: A): F[C]

      @rpc(AvroWithSchema) def unaryWithSchema(a: A): F[C]

      @rpc(Avro, Gzip) def unaryCompressed(a: A): F[C]

      @rpc(AvroWithSchema, Gzip) def unaryCompressedWithSchema(a: A): F[C]

      @rpc(Protobuf) def empty(empty: Empty.type): F[Empty.type]

      @rpc(Protobuf, Gzip) def emptyCompressed(empty: Empty.type): F[Empty.type]

      @rpc(Protobuf) def emptyParam(a: A): F[Empty.type]

      @rpc(Protobuf, Gzip) def emptyParamCompressed(a: A): F[Empty.type]

      @rpc(Protobuf) def emptyParamResponse(empty: Empty.type): F[A]

      @rpc(Protobuf, Gzip) def emptyParamResponseCompressed(empty: Empty.type): F[A]

      @rpc(Avro) def emptyAvro(empty: Empty.type): F[Empty.type]

      @rpc(AvroWithSchema) def emptyAvroWithSchema(empty: Empty.type): F[Empty.type]

      @rpc(Avro, Gzip) def emptyAvroCompressed(empty: Empty.type): F[Empty.type]

      @rpc(AvroWithSchema, Gzip) def emptyAvroWithSchemaCompressed(empty: Empty.type): F[Empty.type]

      @rpc(Avro) def emptyAvroParam(a: A): F[Empty.type]

      @rpc(AvroWithSchema) def emptyAvroWithSchemaParam(a: A): F[Empty.type]

      @rpc(Avro, Gzip) def emptyAvroParamCompressed(a: A): F[Empty.type]

      @rpc(AvroWithSchema, Gzip) def emptyAvroWithSchemaParamCompressed(a: A): F[Empty.type]

      @rpc(Avro) def emptyAvroParamResponse(empty: Empty.type): F[A]

      @rpc(AvroWithSchema) def emptyAvroWithSchemaParamResponse(empty: Empty.type): F[A]

      @rpc(Avro, Gzip) def emptyAvroParamResponseCompressed(empty: Empty.type): F[A]

      @rpc(AvroWithSchema, Gzip) def emptyAvroWithSchemaParamResponseCompressed(
          empty: Empty.type): F[A]

      @rpc(Protobuf)
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): Observable[C]

      @rpc(Protobuf, Gzip)
      @stream[ResponseStreaming.type]
      def serverStreamingCompressed(b: B): Observable[C]

      @rpc(Protobuf)
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Observable[A]): F[D]

      @rpc(Protobuf, Gzip)
      @stream[RequestStreaming.type]
      def clientStreamingCompressed(oa: Observable[A]): F[D]

      @rpc(Avro)
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Observable[E]): Observable[E]

      @rpc(AvroWithSchema)
      @stream[BidirectionalStreaming.type]
      def biStreamingWithSchema(oe: Observable[E]): Observable[E]

      @rpc(Avro, Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressed(oe: Observable[E]): Observable[E]

      @rpc(AvroWithSchema, Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressedWithSchema(oe: Observable[E]): Observable[E]

      @rpc(Protobuf)
      def scope(empty: Empty.type): F[External]

      @rpc(Protobuf, Gzip)
      def scopeCompressed(empty: Empty.type): F[External]

      def sumA(implicit F: cats.Functor[F]): F[Int] =
        F.map(emptyParamResponse(Empty))(a => a.x + a.y)
    }

  }

  object client {

    @tagless(true)
    trait MyRPCClient {
      def notAllowed(b: Boolean): FS[C]
      def empty: FS[Empty.type]
      def emptyParam(a: A): FS[Empty.type]
      def emptyParamResponse: FS[A]
      def emptyAvro: FS[Empty.type]
      def emptyAvroWithSchema: FS[Empty.type]
      def emptyAvroParam(a: A): FS[Empty.type]
      def emptyAvroWithSchemaParam(a: A): FS[Empty.type]
      def emptyAvroParamResponse: FS[A]
      def emptyAvroWithSchemaParamResponse: FS[A]
      def u(x: Int, y: Int): FS[C]
      def uws(x: Int, y: Int): FS[C]
      def ss(a: Int, b: Int): FS[List[C]]
      def cs(cList: List[C], bar: Int): FS[D]
      def bs(eList: List[E]): FS[E]
      def bsws(eList: List[E]): FS[E]
    }

  }

  object handlers {

    object server {

      import database._
      import service._
      import freestyle.rpc.protocol._

      class ServerRPCService[F[_]: Async] extends RPCService[F] {

        def notAllowed(b: Boolean): F[C] = c1.pure

        def empty(empty: Empty.type): F[Empty.type] = Empty.pure

        def emptyParam(a: A): F[Empty.type] = Empty.pure

        def emptyParamResponse(empty: Empty.type): F[A] = a4.pure

        def emptyAvro(empty: Empty.type): F[Empty.type] = Empty.pure

        def emptyAvroWithSchema(empty: Empty.type): F[Empty.type] = emptyAvro(empty)

        def emptyAvroParam(a: A): F[Empty.type] = Empty.pure

        def emptyAvroWithSchemaParam(a: A): F[Empty.type] = emptyAvroParam(a)

        def emptyAvroParamResponse(empty: Empty.type): F[A] = a4.pure

        def emptyAvroWithSchemaParamResponse(empty: Empty.type): F[A] =
          emptyAvroParamResponse(empty)

        def unary(a: A): F[C] = c1.pure

        def unaryWithSchema(a: A): F[C] = unary(a)

        def serverStreaming(b: B): Observable[C] = {
          debug(s"[SERVER] b -> $b")
          Observable.fromIterable(cList)
        }

        def clientStreaming(oa: Observable[A]): F[D] =
          oa.foldLeftL(D(0)) {
              case (current, a) =>
                debug(s"[SERVER] Current -> $current / a -> $a")
                D(current.bar + a.x + a.y)
            }
            .to[F]

        def biStreaming(oe: Observable[E]): Observable[E] =
          oe.flatMap { e: E =>
            save(e)

            Observable.fromIterable(eList)
          }

        def biStreamingWithSchema(oe: Observable[E]): Observable[E] = biStreaming(oe)

        def save(e: E) = e // do something else with e?

        def notAllowedCompressed(b: Boolean): F[C] = notAllowed(b)

        def emptyCompressed(empty: Empty.type): F[Empty.type] = Empty.pure

        def emptyParamCompressed(a: A): F[Empty.type] = emptyParam(a)

        def emptyParamResponseCompressed(empty: Empty.type): F[A] = emptyParamResponse(empty)

        def emptyAvroCompressed(empty: Empty.type): F[Empty.type] = emptyAvro(empty)

        def emptyAvroWithSchemaCompressed(empty: Empty.type): F[Empty.type] =
          emptyAvroCompressed(empty)

        def emptyAvroParamCompressed(a: A): F[Empty.type] = emptyAvroParam(a)

        def emptyAvroWithSchemaParamCompressed(a: A): F[Empty.type] = emptyAvroParamCompressed(a)

        def emptyAvroParamResponseCompressed(empty: Empty.type): F[A] =
          emptyAvroParamResponse(empty)

        def emptyAvroWithSchemaParamResponseCompressed(empty: Empty.type): F[A] =
          emptyAvroParamResponseCompressed(empty)

        def unaryCompressed(a: A): F[C] = unary(a)

        def unaryCompressedWithSchema(a: A): F[C] = unaryCompressed(a)

        def serverStreamingCompressed(b: B): Observable[C] = serverStreaming(b)

        def clientStreamingCompressed(oa: Observable[A]): F[D] = clientStreaming(oa)

        def biStreamingCompressed(oe: Observable[E]): Observable[E] = biStreaming(oe)

        def biStreamingCompressedWithSchema(oe: Observable[E]): Observable[E] =
          biStreamingCompressed(oe)

        import ExternalScope._

        def scope(empty: protocol.Empty.type): F[External] = External(e1).pure

        def scopeCompressed(empty: protocol.Empty.type): F[External] = External(e1).pure
      }

    }

    object client {

      import service._
      import freestyle.rpc.protocol.Utils.client.MyRPCClient
      import freestyle.rpc.protocol._

      class FreesRPCServiceClientHandler[F[_]: Async](
          implicit client: RPCService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient.Handler[F] {

        override def notAllowed(b: Boolean): F[C] =
          client.notAllowed(b)

        override def empty: F[Empty.type] =
          client.empty(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          client.emptyParam(a)

        override def emptyParamResponse: F[A] =
          client.emptyParamResponse(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          client.emptyAvro(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          client.emptyAvroWithSchema(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          client.emptyAvroParam(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          client.emptyAvroWithSchemaParam(a)

        override def emptyAvroParamResponse: F[A] =
          client.emptyAvroParamResponse(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          client.emptyAvroWithSchemaParamResponse(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          client.unary(A(x, y))

        override def uws(x: Int, y: Int): F[C] =
          client.unaryWithSchema(A(x, y))

        override def ss(a: Int, b: Int): F[List[C]] =
          client
            .serverStreaming(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def cs(cList: List[C], bar: Int): F[D] =
          client.clientStreaming(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          client
            .biStreaming(Observable.fromIterable(eList))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]
            .map(_.head)

        override def bsws(eList: List[E]): F[E] =
          client
            .biStreamingWithSchema(Observable.fromIterable(eList))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]
            .map(_.head)

      }

      class FreesRPCServiceClientCompressedHandler[F[_]: Async](
          implicit client: RPCService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient.Handler[F] {

        override def notAllowed(b: Boolean): F[C] =
          client.notAllowedCompressed(b)

        override def empty: F[Empty.type] =
          client.emptyCompressed(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          client.emptyParamCompressed(a)

        override def emptyParamResponse: F[A] =
          client.emptyParamResponseCompressed(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          client.emptyAvroCompressed(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          client.emptyAvroWithSchemaCompressed(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          client.emptyAvroParamCompressed(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          client.emptyAvroWithSchemaParamCompressed(a)

        override def emptyAvroParamResponse: F[A] =
          client.emptyAvroParamResponseCompressed(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          client.emptyAvroWithSchemaParamResponseCompressed(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          client.unaryCompressed(A(x, y))

        override def uws(x: Int, y: Int): F[C] =
          client.unaryCompressedWithSchema(A(x, y))

        override def ss(a: Int, b: Int): F[List[C]] =
          client
            .serverStreamingCompressed(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def cs(cList: List[C], bar: Int): F[D] =
          client.clientStreamingCompressed(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          client
            .biStreamingCompressed(Observable.fromIterable(eList))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]
            .map(_.head)

        override def bsws(eList: List[E]): F[E] =
          client
            .biStreamingCompressedWithSchema(Observable.fromIterable(eList))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]
            .map(_.head)

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
