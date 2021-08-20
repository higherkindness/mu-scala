/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Async, IO, Resource}
import cats.effect.concurrent.Ref
import cats.instances.int._
import fs2.Stream
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.server._
import io.grpc.ServerServiceDefinition
import monix.reactive.Observable
import monix.execution.Scheduler
import natchez._
import org.scalatest.funspec.AnyFunSpec
import scala.util.Random

import Tracing._

class RPCTracingTests extends AnyFunSpec {

  object RPCService {

    case class Request(s: String)

    case class Response(length: Int)

    @service(Protobuf, namespace = Some("com.foo"))
    trait UnaryServiceDef[F[_]] {
      def measureString(req: Request): F[Response]
    }

    @service(Protobuf, namespace = Some("com.foo"))
    trait FS2ServiceDef[F[_]] {
      def fs2ClientStreaming(req: Stream[F, Request]): F[Response]
      def fs2ServerStreaming(req: Request): F[Stream[F, Response]]
      def fs2BidiStreaming(req: Stream[F, Request]): F[Stream[F, Response]]
    }

    @service(Protobuf, namespace = Some("com.foo"))
    trait MonixServiceDef[F[_]] {
      def monixClientStreaming(req: Observable[Request]): F[Response]
      def monixServerStreaming(req: Request): F[Observable[Response]]
      def monixBidiStreaming(req: Observable[Request]): F[Observable[Response]]
    }

    class TracingServiceDef[F[_]: Async: Trace](s: Scheduler)(implicit
        c: Stream.Compiler[F, F]
    ) extends UnaryServiceDef[F]
        with FS2ServiceDef[F]
        with MonixServiceDef[F] {

      implicit val scheduler: Scheduler = s

      def measureString(req: Request): F[Response] =
        Trace[F].span("do something on server side") {
          Response(req.s.length).pure[F]
        }

      def fs2ClientStreaming(req: Stream[F, Request]): F[Response] =
        req.map(_.s.length).compile.foldMonoid.map(Response(_))

      def fs2ServerStreaming(req: Request): F[Stream[F, Response]] =
        Stream(Response(req.s.length)).covary[F].pure[F]

      def fs2BidiStreaming(req: Stream[F, Request]): F[Stream[F, Response]] =
        req.map(r => Response(r.s.length)).pure[F]

      def monixClientStreaming(req: Observable[Request]): F[Response] =
        req.map(_.s.length).foldL.map(Response(_)).toAsync[F]

      def monixServerStreaming(req: Request): F[Observable[Response]] =
        Observable(Response(req.s.length)).pure[F]

      def monixBidiStreaming(req: Observable[Request]): F[Observable[Response]] =
        req.map(r => Response(r.s.length)).pure[F]

    }

  }

  import RPCService._
  import higherkindness.mu.rpc.TestsImplicits._

  val serverPort = 10000 + Random.nextInt(2000)
  val channelFor = ChannelForAddress("localhost", serverPort)

  implicit val tracingService: TracingServiceDef[Kleisli[IO, Span[IO], *]] =
    new TracingServiceDef[Kleisli[IO, Span[IO], *]](Scheduler(EC))

  def serverResource(S: GrpcServer[IO]): Resource[IO, Unit] =
    Resource.make(S.start())(_ => S.shutdown() >> S.awaitTermination())

  /**
   * Build a Resource that starts a server and builds a client to connect to it. The resource
   * finalizer will shut down the server.
   */
  def mkClientResource[C[_[_]]](
      bind: EntryPoint[IO] => IO[ServerServiceDefinition],
      fromChannelFor: ChannelFor => Resource[IO, C[Kleisli[IO, Span[IO], *]]],
      ep: EntryPoint[IO]
  ): Resource[IO, C[Kleisli[IO, Span[IO], *]]] =
    for {
      serviceDef <- Resource.eval(bind(ep))
      grpcServer <- Resource.eval(
        GrpcServer.default[IO](serverPort, List(AddService(serviceDef)))
      )
      _         <- serverResource(grpcServer)
      clientRes <- fromChannelFor(channelFor)
    } yield clientRes

  /*
   * This is the core program common to all the tests:
   * - create a root span
   * - inside the root span, use the client to send a request and receive a response
   * - extract the final tracing log so we can compare it with our expectation
   */
  def program[C[_[_]]](
      clientResource: Resource[IO, C[Kleisli[IO, Span[IO], *]]],
      ep: EntryPoint[IO],
      ref: Ref[IO, TracingData]
  )(
      sendRequest: (C[Kleisli[IO, Span[IO], *]], Span[IO]) => IO[Response]
  ): IO[(Response, List[String])] =
    clientResource.use { client =>
      for {
        resp <- ep.root("client root span").use(span => sendRequest(client, span))
        td   <- ref.get
      } yield (resp, td.log.toList)
    }

  describe("distributed tracing") {

    describe("unary call") {

      it("traces the call on both the client side and server side") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          UnaryServiceDef.bindTracingService[IO],
          UnaryServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            client.measureString(Request("abc")).run(span)
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 3))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.UnaryServiceDef/measureString] (child of 0)",
          "Start span 2 [com.foo.UnaryServiceDef/measureString] (child of 1)",
          "Start span 3 [do something on server side] (child of 2)",
          "End span 3 [do something on server side] (child of 2)",
          "End span 2 [com.foo.UnaryServiceDef/measureString] (child of 1)",
          "End span 1 [com.foo.UnaryServiceDef/measureString] (child of 0)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

    }

    describe("FS2 streaming endpoints") {

      it("traces a client-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          FS2ServiceDef.bindTracingService[IO],
          FS2ServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            client.fs2ClientStreaming(Stream(Request("abc"), Request("defg"))).run(span)
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 7))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.FS2ServiceDef/fs2ClientStreaming] (child of 0)",
          "Start span 2 [com.foo.FS2ServiceDef/fs2ClientStreaming] (child of 1)",
          "End span 2 [com.foo.FS2ServiceDef/fs2ClientStreaming] (child of 1)",
          "End span 1 [com.foo.FS2ServiceDef/fs2ClientStreaming] (child of 0)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

      it("traces a server-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          FS2ServiceDef.bindTracingService[IO],
          FS2ServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            for {
              respStreamK <- client.fs2ServerStreaming(Request("abc")).run(span)
              respStream =
                respStreamK
                  .translateInterruptible(Kleisli.applyK[IO, Span[IO]](span))
              lastResp <- respStream.compile.lastOrError
            } yield lastResp
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 3))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.FS2ServiceDef/fs2ServerStreaming] (child of 0)",
          "End span 1 [com.foo.FS2ServiceDef/fs2ServerStreaming] (child of 0)",
          "Start span 2 [com.foo.FS2ServiceDef/fs2ServerStreaming] (child of 1)",
          "End span 2 [com.foo.FS2ServiceDef/fs2ServerStreaming] (child of 1)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

      it("traces a bidirectional-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          FS2ServiceDef.bindTracingService[IO],
          FS2ServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            val reqStream = Stream(Request("abc"))
            for {
              respStreamK <- client.fs2BidiStreaming(reqStream).run(span)
              respStream =
                respStreamK
                  .translateInterruptible(Kleisli.applyK[IO, Span[IO]](span))
              lastResp <- respStream.compile.lastOrError
            } yield lastResp
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 3))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.FS2ServiceDef/fs2BidiStreaming] (child of 0)",
          "End span 1 [com.foo.FS2ServiceDef/fs2BidiStreaming] (child of 0)",
          "Start span 2 [com.foo.FS2ServiceDef/fs2BidiStreaming] (child of 1)",
          "End span 2 [com.foo.FS2ServiceDef/fs2BidiStreaming] (child of 1)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

    }

    describe("Monix streaming endpoints") {

      implicit val scheduler: Scheduler = Scheduler(EC)

      it("traces a client-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          MonixServiceDef.bindTracingService[IO],
          MonixServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            client.monixClientStreaming(Observable(Request("abc"), Request("defg"))).run(span)
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 7))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.MonixServiceDef/monixClientStreaming] (child of 0)",
          "Start span 2 [com.foo.MonixServiceDef/monixClientStreaming] (child of 1)",
          "End span 2 [com.foo.MonixServiceDef/monixClientStreaming] (child of 1)",
          "End span 1 [com.foo.MonixServiceDef/monixClientStreaming] (child of 0)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

      it("traces a server-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          MonixServiceDef.bindTracingService[IO],
          MonixServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            for {
              respObs  <- client.monixServerStreaming(Request("abc")).run(span)
              lastResp <- respObs.lastL.toAsync[IO]
            } yield lastResp
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 3))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.MonixServiceDef/monixServerStreaming] (child of 0)",
          "End span 1 [com.foo.MonixServiceDef/monixServerStreaming] (child of 0)",
          "Start span 2 [com.foo.MonixServiceDef/monixServerStreaming] (child of 1)",
          "End span 2 [com.foo.MonixServiceDef/monixServerStreaming] (child of 1)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

      it("traces a bidirectional-streaming call") {
        val ref: Ref[IO, TracingData] = Ref.unsafe(TracingData(0, Vector.empty))
        val ep: EntryPoint[IO]        = entrypoint(ref)
        val clientResource = mkClientResource(
          MonixServiceDef.bindTracingService[IO],
          MonixServiceDef.tracingClient[IO](_),
          ep
        )

        val prog: IO[(Response, List[String])] =
          program(clientResource, ep, ref) { case (client, span) =>
            val reqObs = Observable(Request("abc"))
            for {
              respObs  <- client.monixBidiStreaming(reqObs).run(span)
              lastResp <- respObs.lastL.toAsync[IO]
            } yield lastResp
          }
        val (response, log) = prog.unsafeRunSync()

        assert(response == Response(length = 3))

        val expectedLog = List(
          "Start span 0 [client root span] (root)",
          "Start span 1 [com.foo.MonixServiceDef/monixBidiStreaming] (child of 0)",
          "End span 1 [com.foo.MonixServiceDef/monixBidiStreaming] (child of 0)",
          "Start span 2 [com.foo.MonixServiceDef/monixBidiStreaming] (child of 1)",
          "End span 2 [com.foo.MonixServiceDef/monixBidiStreaming] (child of 1)",
          "End span 0 [client root span] (root)"
        )
        assert(log == expectedLog)
      }

    }

  }

}
