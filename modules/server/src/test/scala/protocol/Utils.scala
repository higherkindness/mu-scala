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
import io.grpc.Status
import monix.reactive.Observable

object Utils extends CommonUtils {

  object service {

    @service(Protobuf)
    trait RPCProtobufService[F[_]] {

      import ExternalScope._

      @rpc def notAllowed(b: Boolean): F[C]

      @rpc(Gzip) def notAllowedCompressed(b: Boolean): F[C]

      @rpc def empty(empty: Empty.type): F[Empty.type]

      @rpc(Gzip) def emptyCompressed(empty: Empty.type): F[Empty.type]

      @rpc def emptyParam(a: A): F[Empty.type]

      @rpc(Gzip) def emptyParamCompressed(a: A): F[Empty.type]

      @rpc def emptyParamResponse(empty: Empty.type): F[A]

      @rpc(Gzip) def emptyParamResponseCompressed(empty: Empty.type): F[A]

      @rpc
      @stream[ResponseStreaming.type]
      def serverStreaming(b: B): Observable[C]

      @rpc
      @stream[ResponseStreaming.type]
      def serverStreamingWithError(e: E): Observable[C]

      @rpc(Gzip)
      @stream[ResponseStreaming.type]
      def serverStreamingCompressed(b: B): Observable[C]

      @rpc(Gzip)
      @stream[ResponseStreaming.type]
      def serverStreamingCompressedWithError(e: E): Observable[C]

      @rpc
      @stream[RequestStreaming.type]
      def clientStreaming(oa: Observable[A]): F[D]

      @rpc(Gzip)
      @stream[RequestStreaming.type]
      def clientStreamingCompressed(oa: Observable[A]): F[D]

      @rpc
      def scope(empty: Empty.type): F[External]

      @rpc(Gzip)
      def scopeCompressed(empty: Empty.type): F[External]

      def sumA(implicit F: cats.Functor[F]): F[Int] =
        F.map(emptyParamResponse(Empty))(a => a.x + a.y)
    }

    @service(Avro)
    trait RPCAvroService[F[_]] {

      @rpc def unary(a: A): F[C]

      @rpc def unaryWithError(e: E): F[C]

      @rpc(Gzip) def unaryCompressed(a: A): F[C]

      @rpc(Gzip) def unaryCompressedWithError(e: E): F[C]

      @rpc def emptyAvro(empty: Empty.type): F[Empty.type]

      @rpc(Gzip) def emptyAvroCompressed(empty: Empty.type): F[Empty.type]

      @rpc def emptyAvroParam(a: A): F[Empty.type]

      @rpc(Gzip) def emptyAvroParamCompressed(a: A): F[Empty.type]

      @rpc def emptyAvroParamResponse(empty: Empty.type): F[A]

      @rpc(Gzip) def emptyAvroParamResponseCompressed(empty: Empty.type): F[A]

      @rpc
      @stream[BidirectionalStreaming.type]
      def biStreaming(oe: Observable[E]): Observable[E]

      @rpc(Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressed(oe: Observable[E]): Observable[E]
    }

    @service(AvroWithSchema)
    trait RPCAvroWithSchemaService[F[_]] {

      @rpc def unaryWithSchema(a: A): F[C]

      @rpc(Gzip) def unaryCompressedWithSchema(a: A): F[C]

      @rpc def emptyAvroWithSchema(empty: Empty.type): F[Empty.type]

      @rpc(Gzip) def emptyAvroWithSchemaCompressed(empty: Empty.type): F[Empty.type]

      @rpc def emptyAvroWithSchemaParam(a: A): F[Empty.type]

      @rpc(Gzip) def emptyAvroWithSchemaParamCompressed(a: A): F[Empty.type]

      @rpc def emptyAvroWithSchemaParamResponse(empty: Empty.type): F[A]

      @rpc(Gzip) def emptyAvroWithSchemaParamResponseCompressed(empty: Empty.type): F[A]

      @rpc
      @stream[BidirectionalStreaming.type]
      def biStreamingWithSchema(oe: Observable[E]): Observable[E]

      @rpc(Gzip)
      @stream[BidirectionalStreaming.type]
      def biStreamingCompressedWithSchema(oe: Observable[E]): Observable[E]
    }

  }

  object client {

    @tagless(true)
    trait MyRPCClient[F[_]] {
      def notAllowed(b: Boolean): F[C]
      def empty: F[Empty.type]
      def emptyParam(a: A): F[Empty.type]
      def emptyParamResponse: F[A]
      def emptyAvro: F[Empty.type]
      def emptyAvroWithSchema: F[Empty.type]
      def emptyAvroParam(a: A): F[Empty.type]
      def emptyAvroWithSchemaParam(a: A): F[Empty.type]
      def emptyAvroParamResponse: F[A]
      def emptyAvroWithSchemaParamResponse: F[A]
      def u(x: Int, y: Int): F[C]
      def uws(x: Int, y: Int): F[C]
      def uwe(a: A, err: String): F[C]
      def ss(a: Int, b: Int): F[List[C]]
      def ss192(a: Int, b: Int): F[List[C]]
      def sswe(a: A, err: String): F[List[C]]
      def cs(cList: List[C], bar: Int): F[D]
      def bs(eList: List[E]): F[E]
      def bsws(eList: List[E]): F[E]
    }

  }

  object handlers {

    object server {

      import database._
      import service._
      import freestyle.rpc.protocol._

      class ServerRPCService[F[_]: Async](implicit M: MonadError[F, Throwable])
          extends RPCAvroService[F]
          with RPCAvroWithSchemaService[F]
          with RPCProtobufService[F] {

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

        def unaryWithError(e: E): F[C] = e.foo match {
          case "SE" =>
            M.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asException)
          case "SRE" =>
            M.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asRuntimeException)
          case "RTE" =>
            M.raiseError(new IllegalArgumentException(e.foo))
          case _ =>
            sys.error(e.foo)
        }

        def unaryWithSchema(a: A): F[C] = unary(a)

        def serverStreaming(b: B): Observable[C] = {
          debug(s"[SERVER] b -> $b")
          Observable.fromIterable(cList)
        }

        def serverStreamingWithError(e: E): Observable[C] = e.foo match {
          case "SE" =>
            Observable.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asException)
          case "SRE" =>
            Observable.raiseError(Status.INVALID_ARGUMENT.withDescription(e.foo).asRuntimeException)
          case "RTE" =>
            Observable.raiseError(new IllegalArgumentException(e.foo))
          case _ =>
            sys.error(e.foo)
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

        def unaryCompressedWithError(e: E): F[C] = unaryWithError(e)

        def serverStreamingCompressed(b: B): Observable[C] = serverStreaming(b)

        def serverStreamingCompressedWithError(e: E): Observable[C] = serverStreamingWithError(e)

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
          implicit clientAvro: RPCAvroService.Client[F],
          clientAvroWithSchema: RPCAvroWithSchemaService.Client[F],
          clientProtobuf: RPCProtobufService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient.Handler[F] {

        override def notAllowed(b: Boolean): F[C] =
          clientProtobuf.notAllowed(b)

        override def empty: F[Empty.type] =
          clientProtobuf.empty(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          clientProtobuf.emptyParam(a)

        override def emptyParamResponse: F[A] =
          clientProtobuf.emptyParamResponse(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          clientAvro.emptyAvro(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          clientAvroWithSchema.emptyAvroWithSchema(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          clientAvro.emptyAvroParam(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          clientAvroWithSchema.emptyAvroWithSchemaParam(a)

        override def emptyAvroParamResponse: F[A] =
          clientAvro.emptyAvroParamResponse(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          clientAvroWithSchema.emptyAvroWithSchemaParamResponse(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          clientAvro.unary(A(x, y))

        override def uws(x: Int, y: Int): F[C] =
          clientAvroWithSchema.unaryWithSchema(A(x, y))

        override def uwe(a: A, err: String): F[C] =
          clientAvro.unaryWithError(E(a, err))

        override def ss(a: Int, b: Int): F[List[C]] =
          clientProtobuf
            .serverStreaming(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def ss192(a: Int, b: Int): F[List[C]] =
          clientProtobuf
            .serverStreaming(B(A(a, a), A(b, b)))
            .toListL
            .to[F]

        override def sswe(a: A, err: String): F[List[C]] =
          clientProtobuf
            .serverStreamingWithError(E(a, err))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def cs(cList: List[C], bar: Int): F[D] =
          clientProtobuf.clientStreaming(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          clientAvro
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
          clientAvroWithSchema
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
          implicit clientAvro: RPCAvroService.Client[F],
          clientAvroWithSchema: RPCAvroWithSchemaService.Client[F],
          clientProtobuf: RPCProtobufService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient.Handler[F] {

        override def notAllowed(b: Boolean): F[C] =
          clientProtobuf.notAllowedCompressed(b)

        override def empty: F[Empty.type] =
          clientProtobuf.emptyCompressed(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          clientProtobuf.emptyParamCompressed(a)

        override def emptyParamResponse: F[A] =
          clientProtobuf.emptyParamResponseCompressed(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          clientAvro.emptyAvroCompressed(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          clientAvroWithSchema.emptyAvroWithSchemaCompressed(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          clientAvro.emptyAvroParamCompressed(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          clientAvroWithSchema.emptyAvroWithSchemaParamCompressed(a)

        override def emptyAvroParamResponse: F[A] =
          clientAvro.emptyAvroParamResponseCompressed(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          clientAvroWithSchema.emptyAvroWithSchemaParamResponseCompressed(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          clientAvro.unaryCompressed(A(x, y))

        override def uwe(a: A, err: String): F[C] =
          clientAvro.unaryCompressedWithError(E(a, err))

        override def uws(x: Int, y: Int): F[C] =
          clientAvroWithSchema.unaryCompressedWithSchema(A(x, y))

        override def ss(a: Int, b: Int): F[List[C]] =
          clientProtobuf
            .serverStreamingCompressed(B(A(a, a), A(b, b)))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def ss192(a: Int, b: Int): F[List[C]] =
          clientProtobuf
            .serverStreamingCompressed(B(A(a, a), A(b, b)))
            .toListL
            .to[F]

        override def sswe(a: A, err: String): F[List[C]] =
          clientProtobuf
            .serverStreamingCompressedWithError(E(a, err))
            .zipWithIndex
            .map {
              case (c, i) =>
                debug(s"[CLIENT] Result #$i: $c")
                c
            }
            .toListL
            .to[F]

        override def cs(cList: List[C], bar: Int): F[D] =
          clientProtobuf.clientStreamingCompressed(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          clientAvro
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
          clientAvroWithSchema
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
      AddService(RPCAvroService.bindService[ConcurrentMonad]),
      AddService(RPCAvroWithSchemaService.bindService[ConcurrentMonad]),
      AddService(RPCProtobufService.bindService[ConcurrentMonad])
    )

    implicit val serverW: ServerW = createServerConf(grpcConfigs)

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    implicit val clientAvro: RPCAvroService.Client[ConcurrentMonad] =
      RPCAvroService.client[ConcurrentMonad](createChannelFor)

    implicit val clientAvroWithSchema: RPCAvroWithSchemaService.Client[ConcurrentMonad] =
      RPCAvroWithSchemaService.client[ConcurrentMonad](createChannelFor)

    implicit val clientProtobuf: RPCProtobufService.Client[ConcurrentMonad] =
      RPCProtobufService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
