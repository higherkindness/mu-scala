/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.http

import cats.effect.{IO, _}
import fs2.Stream
import fs2.interop.reactivestreams._
import higherkindness.mu.http.{HttpServer, ResponseError, RouteMap, UnexpectedError}
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import monix.reactive.Observable
import io.circe.generic.auto._
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze._
import org.scalatest._

import scala.concurrent.duration._

class GreeterDerivedRestTests extends RpcBaseTestSuite with BeforeAndAfter {

  val host            = "localhost"
  val port            = 8080
  val serviceUri: Uri = Uri.unsafeFromString(s"http://$host:$port")

  implicit val ec                   = monix.execution.Scheduler.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]     = IO.timer(ec)

  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]
  implicit val fs2HandlerIO   = new Fs2GreeterHandler[IO]
  implicit val monixHandlerIO = new MonixGreeterHandler[IO]

  val unaryRoute: RouteMap[IO] = UnaryGreeter.route[IO]
  val fs2Route: RouteMap[IO]   = Fs2Greeter.route[IO]
  val monixRoute: RouteMap[IO] = MonixGreeter.route[IO]

  val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, unaryRoute, fs2Route, monixRoute)

  var serverTask: Fiber[IO, Nothing] = _
  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
  after(serverTask.cancel)

  "REST Service" should {

    val unaryClient = UnaryGreeter.httpClient[IO](serviceUri)
    val fs2Client   = Fs2Greeter.httpClient[IO](serviceUri)
    val monixClient = MonixGreeter.httpClient[IO](serviceUri)

    "serve a GET request" in {
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(unaryClient.getHello(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "serve a unary POST request" in {
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("hey"))(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "handle a raised gRPC exception in a unary POST request" in {
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("SRE"))(_))

      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.BadRequest,
        Some("INVALID_ARGUMENT: SRE"))
    }

    "handle a raised non-gRPC exception in a unary POST request" in {
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("RTE"))(_))

      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.InternalServerError,
        Some("RTE"))
    }

    "handle a thrown exception in a unary POST request" in {
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("TR"))(_))

      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.InternalServerError)
    }

    "serve a POST request with fs2 streaming request" in {

      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))

      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(fs2Client.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty fs2 streaming request" in {
      val requests = Stream.empty
      val response =
        BlazeClientBuilder[IO](ec).resource.use(fs2Client.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with Observable streaming request" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val response =
        BlazeClientBuilder[IO](ec).resource.use(monixClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty Observable streaming request" in {
      val requests = Observable.empty
      val response =
        BlazeClientBuilder[IO](ec).resource.use(monixClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with fs2 streaming response" in {
      val request = HelloRequest("hey")
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2Client.sayHelloAll(request)(_))
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "serve a POST request with Observable streaming response" in {
      val request = HelloRequest("hey")
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixClient.sayHelloAll(request)(_).toReactivePublisher.toStream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "handle errors with fs2 streaming response" in {
      val request = HelloRequest("")
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2Client.sayHelloAll(request)(_))
      the[UnexpectedError] thrownBy responses.compile.toList
        .unsafeRunSync() should have message "java.lang.IllegalArgumentException: empty greeting"
    }

    "handle errors with Observable streaming response" in {
      val request = HelloRequest("")
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixClient.sayHelloAll(request)(_).toReactivePublisher.toStream[IO])
      the[UnexpectedError] thrownBy responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) should have message "java.lang.IllegalArgumentException: empty greeting"
    }

    "serve a POST request with bidirectional fs2 streaming" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2Client.sayHellosAll(requests)(_))
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional fs2 streaming" in {
      val requests = Stream.empty
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2Client.sayHellosAll(requests)(_))
      responses.compile.toList.unsafeRunSync() shouldBe Nil
    }

    "serve a POST request with bidirectional Observable streaming" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixClient.sayHellosAll(requests)(_).toReactivePublisher.toStream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional Observable streaming" in {
      val requests = Observable.empty
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixClient.sayHellosAll(requests)(_).toReactivePublisher.toStream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe Nil
    }

  }

}
