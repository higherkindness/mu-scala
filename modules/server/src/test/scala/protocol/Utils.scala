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

import cats.MonadError
import cats.effect.Async
import cats.syntax.applicative._
import mu.rpc.common._
import io.grpc.Status
import monix.execution.Scheduler
import monix.reactive.Observable

object Utils extends CommonUtils {

  object service {
    @service(Avro, Gzip) trait CompressedAvroRPCService[F[_]] {
      def unaryCompressed(a: A): F[C]
      def unaryCompressedWithError(e: E): F[C]
      def emptyAvroCompressed(empty: Empty.type): F[Empty.type]
      def emptyAvroParamCompressed(a: A): F[Empty.type]
      def emptyAvroParamResponseCompressed(empty: Empty.type): F[A]
      def biStreamingCompressed(oe: Observable[E]): Observable[E]
    }

    @service(Protobuf, Gzip) trait CompressedProtoRPCService[F[_]] {
      import ExternalScope._

      def notAllowedCompressed(b: Boolean): F[C]
      def emptyCompressed(empty: Empty.type): F[Empty.type]
      def emptyParamCompressed(a: A): F[Empty.type]
      def emptyParamResponseCompressed(empty: Empty.type): F[A]
      def serverStreamingCompressed(b: B): Observable[C]
      def serverStreamingCompressedWithError(e: E): Observable[C]
      def clientStreamingCompressed(oa: Observable[A]): F[D]
      def scopeCompressed(empty: Empty.type): F[External]
    }

    @service(AvroWithSchema, Gzip) trait CompressedAvroWithSchemaRPCService[F[_]] {
      def unaryCompressedWithSchema(a: A): F[C]
      def emptyAvroWithSchemaCompressed(empty: Empty.type): F[Empty.type]
      def emptyAvroWithSchemaParamCompressed(a: A): F[Empty.type]
      def emptyAvroWithSchemaParamResponseCompressed(empty: Empty.type): F[A]
      def biStreamingCompressedWithSchema(oe: Observable[E]): Observable[E]
    }

    @service(Avro) trait AvroRPCService[F[_]] {
      def unary(a: A): F[C]
      def unaryWithError(e: E): F[C]
      def emptyAvro(empty: Empty.type): F[Empty.type]
      def emptyAvroParam(a: A): F[Empty.type]
      def emptyAvroParamResponse(empty: Empty.type): F[A]
      def biStreaming(oe: Observable[E]): Observable[E]
    }

    @service(Protobuf) trait ProtoRPCService[F[_]] {
      import ExternalScope._

      def notAllowed(b: Boolean): F[C]
      def empty(empty: Empty.type): F[Empty.type]
      def emptyParam(a: A): F[Empty.type]
      def emptyParamResponse(empty: Empty.type): F[A]
      def serverStreaming(b: B): Observable[C]
      def serverStreamingWithError(e: E): Observable[C]
      def clientStreaming(oa: Observable[A]): F[D]
      def scope(empty: Empty.type): F[External]

      def sumA(implicit F: cats.Functor[F]): F[Int] =
        F.map(emptyParamResponse(Empty))(a => a.x + a.y)
    }

    @service(AvroWithSchema) trait AvroWithSchemaRPCService[F[_]] {
      def unaryWithSchema(a: A): F[C]
      def emptyAvroWithSchema(empty: Empty.type): F[Empty.type]
      def emptyAvroWithSchemaParam(a: A): F[Empty.type]
      def emptyAvroWithSchemaParamResponse(empty: Empty.type): F[A]
      def biStreamingWithSchema(oe: Observable[E]): Observable[E]
    }
  }

  object client {

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
      import mu.rpc.protocol._

      class ServerRPCService[F[_]: Async](implicit M: MonadError[F, Throwable])
          extends ProtoRPCService[F]
          with AvroRPCService[F]
          with AvroWithSchemaRPCService[F]
          with CompressedProtoRPCService[F]
          with CompressedAvroRPCService[F]
          with CompressedAvroWithSchemaRPCService[F] {

        implicit val S: Scheduler = Scheduler(EC)

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
      import mu.rpc.protocol.Utils.client.MyRPCClient
      import mu.rpc.protocol._

      class FreesRPCServiceClientHandler[F[_]: Async](
          implicit
          proto: ProtoRPCService.Client[F],
          aws: AvroWithSchemaRPCService.Client[F],
          avro: AvroRPCService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient[F] {

        implicit val S: Scheduler = Scheduler(EC)

        override def notAllowed(b: Boolean): F[C] =
          proto.notAllowed(b)

        override def empty: F[Empty.type] =
          proto.empty(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          proto.emptyParam(a)

        override def emptyParamResponse: F[A] =
          proto.emptyParamResponse(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          avro.emptyAvro(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          aws.emptyAvroWithSchema(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          avro.emptyAvroParam(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          aws.emptyAvroWithSchemaParam(a)

        override def emptyAvroParamResponse: F[A] =
          avro.emptyAvroParamResponse(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          aws.emptyAvroWithSchemaParamResponse(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          avro.unary(A(x, y))

        override def uws(x: Int, y: Int): F[C] =
          aws.unaryWithSchema(A(x, y))

        override def uwe(a: A, err: String): F[C] =
          avro.unaryWithError(E(a, err))

        override def ss(a: Int, b: Int): F[List[C]] =
          proto
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
          proto
            .serverStreaming(B(A(a, a), A(b, b)))
            .toListL
            .to[F]

        override def sswe(a: A, err: String): F[List[C]] =
          proto
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
          proto.clientStreaming(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          avro
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
          aws
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
          implicit proto: CompressedProtoRPCService.Client[F],
          aws: CompressedAvroWithSchemaRPCService.Client[F],
          avro: CompressedAvroRPCService.Client[F],
          M: MonadError[F, Throwable])
          extends MyRPCClient[F] {

        implicit val S: Scheduler = Scheduler(EC)

        override def notAllowed(b: Boolean): F[C] =
          proto.notAllowedCompressed(b)

        override def empty: F[Empty.type] =
          proto.emptyCompressed(protocol.Empty)

        override def emptyParam(a: A): F[Empty.type] =
          proto.emptyParamCompressed(a)

        override def emptyParamResponse: F[A] =
          proto.emptyParamResponseCompressed(protocol.Empty)

        override def emptyAvro: F[Empty.type] =
          avro.emptyAvroCompressed(protocol.Empty)

        override def emptyAvroWithSchema: F[Empty.type] =
          aws.emptyAvroWithSchemaCompressed(protocol.Empty)

        override def emptyAvroParam(a: A): F[Empty.type] =
          avro.emptyAvroParamCompressed(a)

        override def emptyAvroWithSchemaParam(a: A): F[Empty.type] =
          aws.emptyAvroWithSchemaParamCompressed(a)

        override def emptyAvroParamResponse: F[A] =
          avro.emptyAvroParamResponseCompressed(protocol.Empty)

        override def emptyAvroWithSchemaParamResponse: F[A] =
          aws.emptyAvroWithSchemaParamResponseCompressed(protocol.Empty)

        override def u(x: Int, y: Int): F[C] =
          avro.unaryCompressed(A(x, y))

        override def uwe(a: A, err: String): F[C] =
          avro.unaryCompressedWithError(E(a, err))

        override def uws(x: Int, y: Int): F[C] =
          aws.unaryCompressedWithSchema(A(x, y))

        override def ss(a: Int, b: Int): F[List[C]] =
          proto
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
          proto
            .serverStreamingCompressed(B(A(a, a), A(b, b)))
            .toListL
            .to[F]

        override def sswe(a: A, err: String): F[List[C]] =
          proto
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
          proto.clientStreamingCompressed(Observable.fromIterable(cList.map(c => c.a)))

        import cats.syntax.functor._
        override def bs(eList: List[E]): F[E] =
          avro
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
          aws
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
    import mu.rpc.server._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCHandler: ServerRPCService[ConcurrentMonad] =
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

    implicit val protoRPCServiceClient: ProtoRPCService.Client[ConcurrentMonad] =
      ProtoRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val avroRPCServiceClient: AvroRPCService.Client[ConcurrentMonad] =
      AvroRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val awsRPCServiceClient: AvroWithSchemaRPCService.Client[ConcurrentMonad] =
      AvroWithSchemaRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val compressedprotoRPCServiceClient: CompressedProtoRPCService.Client[
      ConcurrentMonad] =
      CompressedProtoRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val compressedavroRPCServiceClient: CompressedAvroRPCService.Client[ConcurrentMonad] =
      CompressedAvroRPCService.client[ConcurrentMonad](createChannelFor)
    implicit val compressedawsRPCServiceClient: CompressedAvroWithSchemaRPCService.Client[
      ConcurrentMonad] =
      CompressedAvroWithSchemaRPCService.client[ConcurrentMonad](createChannelFor)

  }

  object implicits extends FreesRuntime

}
