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

package freestyle.rpc.http

import cats.effect.IO
import freestyle.rpc.common.RpcBaseTestSuite
import freestyle.rpc.http.Utils._
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import monix.reactive.Observable
import org.http4s._
import org.http4s.circe._
import org.http4s.client.UnexpectedStatus
import org.http4s.client.blaze.Http1Client
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.Logger
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.concurrent.duration._

class GreeterRestTests
    extends RpcBaseTestSuite
    with GeneratorDrivenPropertyChecks
    with BeforeAndAfter {

  val Hostname = "localhost"
  val Port     = 8080

  val serviceUri: Uri = Uri.unsafeFromString(s"http://$Hostname:$Port")

  val UnaryServicePrefix = "unary"
  val Fs2ServicePrefix   = "fs2"
  val MonixServicePrefix = "monix"

  import monix.execution.Scheduler.Implicits.global

  val unaryService: HttpService[IO] = Logger(logHeaders = true, logBody = true)(
    new UnaryGreeterRestService[IO](new UnaryGreeterHandler[IO]).service)
  val fs2Service: HttpService[IO] = Logger(logHeaders = true, logBody = true)(
    new Fs2GreeterRestService[IO](new Fs2GreeterHandler[IO]).service)
  val monixService: HttpService[IO] = Logger(logHeaders = true, logBody = true)(
    new MonixGreeterRestService[IO](new MonixGreeterHandler[IO]).service)

  val server: BlazeBuilder[IO] =
    BlazeBuilder[IO]
      .bindHttp(Port, Hostname)
      //.enableHttp2(true)
      //.withSSLContext(GenericSSLContext.serverSSLContext)
      .mountService(unaryService, s"/$UnaryServicePrefix")
      .mountService(fs2Service, s"/$Fs2ServicePrefix")
      .mountService(monixService, s"/$MonixServicePrefix")

  var serverTask: Server[IO] = _ // sorry
  before(serverTask = server.start.unsafeRunSync())
  after(serverTask.shutdownNow())

  "REST Server" should {

    "serve a GET request" in {
      val request = Request[IO](Method.GET, serviceUri / UnaryServicePrefix / "getHello")
      val response = for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
    }

    "serve a POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
      val requestBody = HelloRequest("hey").asJson
      val response = for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
    }

    "return a 400 Bad Request for a malformed unary POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
      val requestBody = "{"
      val response = for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response
      the[UnexpectedStatus] thrownBy response.unsafeRunSync() shouldBe UnexpectedStatus(
        Status.BadRequest)
    }

    "return a 400 Bad Request for a malformed streaming POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / Fs2ServicePrefix / "sayHellos")
      val requestBody = "{"
      val response = for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response
      the[UnexpectedStatus] thrownBy response.unsafeRunSync() shouldBe UnexpectedStatus(
        Status.BadRequest)
    }

  }

  val unaryServiceClient: UnaryGreeterRestClient[IO] =
    new UnaryGreeterRestClient[IO](serviceUri / UnaryServicePrefix)
  val fs2ServiceClient: Fs2GreeterRestClient[IO] =
    new Fs2GreeterRestClient[IO](serviceUri / Fs2ServicePrefix)
  val monixServiceClient: MonixGreeterRestClient[IO] =
    new MonixGreeterRestClient[IO](serviceUri / MonixServicePrefix)

  "REST Service" should {

    "serve a GET request" in {
      val response = for {
        client   <- Http1Client[IO]()
        response <- unaryServiceClient.getHello()(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "serve a unary POST request" in {
      val request = HelloRequest("hey")
      val response = for {
        client   <- Http1Client[IO]()
        response <- unaryServiceClient.sayHello(request)(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "handle a raised gRPC exception in a unary POST request" in {
      val request = HelloRequest("SRE")
      val response = for {
        client   <- Http1Client[IO]()
        response <- unaryServiceClient.sayHello(request)(client)
      } yield response
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe
        ResponseError(Status.BadRequest, Some("INVALID_ARGUMENT: SRE"))
    }

    "handle a raised non-gRPC exception in a unary POST request" in {
      val request = HelloRequest("RTE")
      val response = for {
        client   <- Http1Client[IO]()
        response <- unaryServiceClient.sayHello(request)(client)
      } yield response
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe
        ResponseError(Status.InternalServerError, Some("RTE"))
    }

    "handle a thrown exception in a unary POST request" in {
      val request = HelloRequest("TR")
      val response = for {
        client   <- Http1Client[IO]()
        response <- unaryServiceClient.sayHello(request)(client)
      } yield response
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe
        ResponseError(Status.InternalServerError)
    }

    "serve a POST request with fs2 streaming request" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val response = for {
        client   <- Http1Client[IO]()
        response <- fs2ServiceClient.sayHellos(requests)(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty fs2 streaming request" in {
      val requests = Stream.empty
      val response = for {
        client   <- Http1Client[IO]()
        response <- fs2ServiceClient.sayHellos(requests)(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with Observable streaming request" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val response = for {
        client   <- Http1Client[IO]()
        response <- monixServiceClient.sayHellos(requests)(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty Observable streaming request" in {
      val requests = Observable.empty
      val response = for {
        client   <- Http1Client[IO]()
        response <- monixServiceClient.sayHellos(requests)(client)
      } yield response
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with fs2 streaming response" in {
      val request = HelloRequest("hey")
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- fs2ServiceClient.sayHelloAll(request)(client)
      } yield responses
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "serve a POST request with Observable streaming response" in {
      val request = HelloRequest("hey")
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- monixServiceClient.sayHelloAll(request)(client).toFs2Stream[IO]
      } yield responses
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "handle errors with fs2 streaming response" in {
      val request = HelloRequest("")
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- fs2ServiceClient.sayHelloAll(request)(client)
      } yield responses
      the[IllegalArgumentException] thrownBy responses.compile.toList
        .unsafeRunSync() should have message "empty greeting"
    }

    "handle errors with Observable streaming response" in {
      val request = HelloRequest("")
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- monixServiceClient.sayHelloAll(request)(client).toFs2Stream[IO]
      } yield responses
      the[IllegalArgumentException] thrownBy responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) should have message "empty greeting"
    }

    "serve a POST request with bidirectional fs2 streaming" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- fs2ServiceClient.sayHellosAll(requests)(client)
      } yield responses
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional fs2 streaming" in {
      val requests = Stream.empty
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- fs2ServiceClient.sayHellosAll(requests)(client)
      } yield responses
      responses.compile.toList
        .unsafeRunSync() shouldBe Nil
    }

    "serve a POST request with bidirectional Observable streaming" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- monixServiceClient.sayHellosAll(requests)(client).toFs2Stream[IO]
      } yield responses
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional Observable streaming" in {
      val requests = Observable.empty
      val responses = for {
        client    <- Http1Client.stream[IO]()
        responses <- monixServiceClient.sayHellosAll(requests)(client).toFs2Stream[IO]
      } yield responses
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe Nil
    }

    "serve ScalaCheck-generated POST requests with bidirectional Observable streaming" in {
      forAll { strings: List[String] =>
        val requests = Observable.fromIterable(strings.map(HelloRequest))
        val responses = for {
          client    <- Http1Client.stream[IO]()
          responses <- monixServiceClient.sayHellosAll(requests)(client).toFs2Stream[IO]
        } yield responses
        responses.compile.toList
          .unsafeRunTimed(10.seconds)
          .getOrElse(sys.error("Stuck!")) shouldBe strings.map(HelloResponse)
      }
    }

  }

}
