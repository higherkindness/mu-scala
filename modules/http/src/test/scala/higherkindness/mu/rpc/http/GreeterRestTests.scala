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
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.http.Utils._
import higherkindness.mu.rpc.protocol.Empty
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import monix.reactive.Observable
import org.http4s._
import org.http4s.circe._
import org.http4s.client.UnexpectedStatus
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.server.blaze._
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

  implicit val ec                   = monix.execution.Scheduler.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  //TODO: add Logger middleware
  val unaryService: HttpRoutes[IO] =
    new UnaryGreeterRestService[IO](new UnaryGreeterHandler[IO]).service
  val fs2Service: HttpRoutes[IO] =
    new Fs2GreeterRestService[IO](new Fs2GreeterHandler[IO]).service
  val monixService: HttpRoutes[IO] =
    new MonixGreeterRestService[IO](new MonixGreeterHandler[IO]).service

  val server: BlazeServerBuilder[IO] = BlazeServerBuilder[IO]
    .bindHttp(Port, Hostname)
    //.enableHttp2(true)
    //.withSSLContext(GenericSSLContext.serverSSLContext)
    .withHttpApp(
      Router(
        s"/$UnaryServicePrefix" -> unaryService,
        s"/$Fs2ServicePrefix"   -> fs2Service,
        s"/$MonixServicePrefix" -> monixService).orNotFound)

  var serverTask: Fiber[IO, Nothing] = _ // sorry
  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
  after(serverTask.cancel)

  "REST Server" should {

    "serve a GET request" in {
      val request  = Request[IO](Method.GET, serviceUri / UnaryServicePrefix / "getHello")
      val response = BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request))
      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
    }

    "serve a POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
      val requestBody = HelloRequest("hey").asJson
      val response =
        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
    }

    "return a 400 Bad Request for a malformed unary POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
      val requestBody = "{"
      val response =
        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
      the[UnexpectedStatus] thrownBy response.unsafeRunSync() shouldBe UnexpectedStatus(
        Status.BadRequest)
    }

    "return a 400 Bad Request for a malformed streaming POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / Fs2ServicePrefix / "sayHellos")
      val requestBody = "{"
      val response =
        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
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
      val response = BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.getHello()(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "serve a unary POST request" in {
      val request = HelloRequest("hey")
      val response =
        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "handle a raised gRPC exception in a unary POST request" in {
      val request = HelloRequest("SRE")
      val response =
        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.BadRequest,
        Some("INVALID_ARGUMENT: SRE"))
    }

    "handle a raised non-gRPC exception in a unary POST request" in {
      val request = HelloRequest("RTE")
      val response =
        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.InternalServerError,
        Some("RTE"))
    }

    "handle a thrown exception in a unary POST request" in {
      val request = HelloRequest("TR")
      val response =
        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
        Status.InternalServerError)
    }

    "serve a POST request with fs2 streaming request" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val response =
        BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty fs2 streaming request" in {
      val requests = Stream.empty
      val response =
        BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with Observable streaming request" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val response =
        BlazeClientBuilder[IO](ec).resource.use(monixServiceClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty Observable streaming request" in {
      val requests = Observable.empty
      val response =
        BlazeClientBuilder[IO](ec).resource.use(monixServiceClient.sayHellos(requests)(_))
      response.unsafeRunSync() shouldBe HelloResponse("")
    }

    "serve a POST request with fs2 streaming response" in {
      val request = HelloRequest("hey")
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "serve a POST request with Observable streaming response" in {
      val request = HelloRequest("hey")
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixServiceClient.sayHelloAll(request)(_).toFs2Stream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "handle errors with fs2 streaming response" in {
      val request = HelloRequest("")
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
      the[IllegalArgumentException] thrownBy responses.compile.toList
        .unsafeRunSync() should have message "empty greeting"
    }

    "handle errors with Observable streaming response" in {
      val request = HelloRequest("")
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixServiceClient.sayHelloAll(request)(_).toFs2Stream[IO])
      the[IllegalArgumentException] thrownBy responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) should have message "empty greeting"
    }

    "serve a POST request with bidirectional fs2 streaming" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
      responses.compile.toList
        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional fs2 streaming" in {
      val requests = Stream.empty
      val responses =
        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
      responses.compile.toList.unsafeRunSync() shouldBe Nil
    }

    "serve a POST request with bidirectional Observable streaming" in {
      val requests = Observable(HelloRequest("hey"), HelloRequest("there"))
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixServiceClient.sayHellosAll(requests)(_).toFs2Stream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }

    "serve an empty POST request with bidirectional Observable streaming" in {
      val requests = Observable.empty
      val responses = BlazeClientBuilder[IO](ec).stream
        .flatMap(monixServiceClient.sayHellosAll(requests)(_).toFs2Stream[IO])
      responses.compile.toList
        .unsafeRunTimed(10.seconds)
        .getOrElse(sys.error("Stuck!")) shouldBe Nil
    }

    "serve ScalaCheck-generated POST requests with bidirectional Observable streaming" in {
      forAll { strings: List[String] =>
        val requests = Observable.fromIterable(strings.map(HelloRequest))
        val responses = BlazeClientBuilder[IO](ec).stream
          .flatMap(monixServiceClient.sayHellosAll(requests)(_).toFs2Stream[IO])
        responses.compile.toList
          .unsafeRunTimed(10.seconds)
          .getOrElse(sys.error("Stuck!")) shouldBe strings.map(HelloResponse)
      }
    }
  }

  "Auto-derived REST Client" should {

    "serve a GET request" in {
      val client: UnaryGreeter.HttpClient[IO] = UnaryGreeter.httpClient[IO](serviceUri)
      val response: IO[HelloResponse]         = BlazeClientBuilder[IO](ec).resource.use(client.getHello(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    "serve a unary POST request" in {
      val client: UnaryGreeter.HttpClient[IO] = UnaryGreeter.httpClient[IO](serviceUri)
      val response: IO[HelloResponse] =
        BlazeClientBuilder[IO](ec).resource.use(client.sayHello(HelloRequest("hey"))(_))
      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

    //TODO: more tests
  }

}
