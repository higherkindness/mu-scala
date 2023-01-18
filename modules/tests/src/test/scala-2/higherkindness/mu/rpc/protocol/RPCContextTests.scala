/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.protocol

import cats.data.Kleisli
import cats.effect.{Async, IO, Resource}
import cats.syntax.all._
import fs2.Stream
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.internal.context.{ClientContext, ClientContextMetaData, ServerContext}
import higherkindness.mu.rpc.server._
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor, ServerServiceDefinition}
import munit.CatsEffectSuite

import scala.util.Random

class RPCContextTests extends CatsEffectSuite {

  object RPCService {

    case class Request(s: String)

    case class Response1(length: Int, key1: String, key2: String)
    case class Response2(length: Int)

    case class ServiceContext(metadata: Metadata)

    @service(Protobuf, namespace = Some("com.foo"))
    trait UnaryServiceDef[F[_]] {
      def measureString(req: Request): F[Response1]
    }

    @service(Protobuf, namespace = Some("com.foo"))
    trait FS2ServiceDef[F[_]] {
      def fs2ClientStreaming(req: Stream[F, Request]): F[Response2]
      def fs2ServerStreaming(req: Request): F[Stream[F, Response2]]
      def fs2BidiStreaming(req: Stream[F, Request]): F[Stream[F, Response2]]
    }

    class ContextServiceDef[F[_]: Async]() extends UnaryServiceDef[Kleisli[F, ServiceContext, *]] {

      def measureString(req: Request): Kleisli[F, ServiceContext, Response1] = Kleisli { context =>
        Response1(req.s.length, context.metadata.get(key1), context.metadata.get(key2)).pure[F]
      }
    }

    class ContextFS2ServiceDef[F[_]: Async]() extends FS2ServiceDef[F] {

      def fs2ClientStreaming(req: Stream[F, Request]): F[Response2] =
        req.map(_.s.length).compile.foldMonoid.map(Response2(_))

      def fs2ServerStreaming(req: Request): F[Stream[F, Response2]] =
        Stream(Response2(req.s.length)).covary[F].pure[F]

      def fs2BidiStreaming(req: Stream[F, Request]): F[Stream[F, Response2]] =
        req.map(r => Response2(r.s.length)).pure[F]
    }

  }

  import RPCService._

  val serverPort = 10000 + Random.nextInt(2000)
  val channelFor = ChannelForAddress("localhost", serverPort)

  implicit val contextService: UnaryServiceDef[Kleisli[IO, ServiceContext, *]] =
    new ContextServiceDef[IO]()

  implicit val contextFS2Service: FS2ServiceDef[Kleisli[IO, ServiceContext, *]] =
    new ContextFS2ServiceDef[Kleisli[IO, ServiceContext, *]]()

  implicit val serverContext: ServerContext[IO, ServiceContext] =
    new ServerContext[IO, ServiceContext] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          metadata: Metadata
      ): Resource[IO, ServiceContext] =
        Resource.pure[IO, ServiceContext](ServiceContext(metadata))
    }

  implicit val clientContext: ClientContext[IO, ServiceContext] =
    new ClientContext[IO, ServiceContext] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          channel: Channel,
          options: CallOptions,
          current: ServiceContext
      ): Resource[IO, ClientContextMetaData[ServiceContext]] = {
        val metadata = current.metadata
        metadata.put(key2, "value2")
        Resource.pure[IO, ClientContextMetaData[ServiceContext]](
          ClientContextMetaData(current, metadata)
        )
      }
    }

  private val key1: Metadata.Key[String] = Metadata.Key.of("key1", Metadata.ASCII_STRING_MARSHALLER)
  private val key2: Metadata.Key[String] = Metadata.Key.of("key2", Metadata.ASCII_STRING_MARSHALLER)

  def serverResource(S: GrpcServer[IO]): Resource[IO, Unit] =
    Resource.make(S.start)(_ => S.shutdown >> S.awaitTermination)

  /**
   * Build a Resource that starts a server and builds a client to connect to it. The resource
   * finalizer will shut down the server.
   */
  def mkClientResource[C[_[_]]](
      bind: Resource[IO, ServerServiceDefinition],
      fromChannelFor: ChannelFor => Resource[IO, UnaryServiceDef[Kleisli[IO, ServiceContext, *]]]
  ): Resource[IO, UnaryServiceDef[Kleisli[IO, ServiceContext, *]]] =
    for {
      serviceDef <- bind
      _          <- GrpcServer.defaultServer[IO](serverPort, List(AddService(serviceDef)))
      clientRes  <- fromChannelFor(channelFor)
    } yield clientRes

  test(
    "distributed context with unary call works as expected"
  ) {
    val clientResource: Resource[IO, UnaryServiceDef[Kleisli[IO, ServiceContext, *]]] = for {
      serviceDef <- UnaryServiceDef.bindContextService[IO, ServiceContext]
      _          <- GrpcServer.defaultServer[IO](serverPort, List(AddService(serviceDef)))
      clientRes  <- UnaryServiceDef.contextClient[IO, ServiceContext](channelFor)
    } yield clientRes

    val prog: IO[Response1] =
      clientResource.use { client =>
        val metadata: Metadata = new Metadata()
        metadata.put(key1, "value1")
        val context: ServiceContext = ServiceContext(metadata)
        client.measureString(Request("abc")).run(context)
      }

    prog.map { response =>
      assertEquals(response, Response1(length = 3, "value1", "value2"))
    }
  }

  test("distributed context with FS2 streaming endpoints works as expected") {
    val clientResource: Resource[IO, FS2ServiceDef[Kleisli[IO, ServiceContext, *]]] = for {
      serviceDef <- FS2ServiceDef.bindContextService[IO, ServiceContext]
      _          <- GrpcServer.defaultServer[IO](serverPort, List(AddService(serviceDef)))
      clientRes  <- FS2ServiceDef.contextClient[IO, ServiceContext](channelFor)
    } yield clientRes

    val prog: IO[Response2] =
      clientResource.use { client =>
        val context: ServiceContext = ServiceContext(new Metadata())
        client.fs2ClientStreaming(Stream(Request("abc"), Request("defg"))).run(context)
      }
    prog.map(assertEquals(_, Response2(length = 7)))
  }

  test("distributed context with FS2 streaming endpoints works as expected") {
    val clientResource: Resource[IO, FS2ServiceDef[Kleisli[IO, ServiceContext, *]]] = for {
      serviceDef <- FS2ServiceDef.bindContextService[IO, ServiceContext]
      _          <- GrpcServer.defaultServer[IO](serverPort, List(AddService(serviceDef)))
      clientRes  <- FS2ServiceDef.contextClient[IO, ServiceContext](channelFor)
    } yield clientRes

    val prog: IO[Response2] =
      clientResource.use { client =>
        val context: ServiceContext = ServiceContext(new Metadata())
        for {
          respStreamK <- client.fs2ServerStreaming(Request("abc")).run(context)
          respStream =
            respStreamK
              .translate(Kleisli.applyK[IO, ServiceContext](context))
          lastResp <- respStream.compile.lastOrError
        } yield lastResp
      }
    prog.map(assertEquals(_, Response2(length = 3)))
  }

  test("distributed tracing with FS2 streaming endpoints traces a bidirectional-streaming call") {
    val clientResource: Resource[IO, FS2ServiceDef[Kleisli[IO, ServiceContext, *]]] = for {
      serviceDef <- FS2ServiceDef.bindContextService[IO, ServiceContext]
      _          <- GrpcServer.defaultServer[IO](serverPort, List(AddService(serviceDef)))
      clientRes  <- FS2ServiceDef.contextClient[IO, ServiceContext](channelFor)
    } yield clientRes

    val prog: IO[Response2] =
      clientResource.use { client =>
        val context: ServiceContext = ServiceContext(new Metadata())
        val reqStream               = Stream(Request("abc"))
        for {
          respStreamK <- client.fs2BidiStreaming(reqStream).run(context)
          respStream =
            respStreamK
              .translate(Kleisli.applyK[IO, ServiceContext](context))
          lastResp <- respStream.compile.lastOrError
        } yield lastResp
      }
    prog.map(assertEquals(_, Response2(length = 3)))
  }

}
