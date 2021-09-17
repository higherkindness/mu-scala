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

package higherkindness.mu.rpc.http

import cats.effect.{IO, _}
import fs2.Stream
import higherkindness.mu.http.{ResponseError, UnexpectedError}
import higherkindness.mu.http.implicits._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.client.UnexpectedStatus
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server._
import org.http4s.implicits._
import org.http4s.server.Router

abstract class GreeterRestTests extends CatsEffectSuite {

  val Hostname = "localhost"
  val Port     = 8080

  val serviceUri: Uri = Uri.unsafeFromString(s"http://$Hostname:$Port")

  val UnaryServicePrefix = "UnaryGreeter"
  val Fs2ServicePrefix   = "Fs2Greeter"
  val MonixServicePrefix = "MonixGreeter"

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]
  implicit val fs2HandlerIO   = new Fs2GreeterHandler[IO]

  val unaryService: HttpRoutes[IO] = new UnaryGreeterRestService[IO].service
  val fs2Service: HttpRoutes[IO]   = new Fs2GreeterRestService[IO].service

  val server: BlazeServerBuilder[IO] = BlazeServerBuilder[IO](ec)
    .bindHttp(Port, Hostname)
    .withHttpApp(
      Router(
        s"/$UnaryServicePrefix" -> unaryService,
        s"/$Fs2ServicePrefix"   -> fs2Service
      ).orNotFound
    )

  var serverTask: FiberIO[Unit] = _

  override def beforeAll(): Unit =
    serverTask = server.resource.use(_ => IO.never[Unit]).start.unsafeRunSync()

  override def afterAll(): Unit = serverTask.cancel.unsafeRunSync()

  test("REST Server should serve a GET request") {
    val request  = Request[IO](Method.GET, serviceUri / UnaryServicePrefix / "getHello")
    val response = BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request))
    response.assertEquals(HelloResponse("hey").asJson)
  }

  test("REST Server should serve a POST request") {
    val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
    val requestBody = HelloRequest("hey").asJson
    val response =
      BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
    response.assertEquals(HelloResponse("hey").asJson)
  }

  test("REST Server should return a 400 Bad Request for a malformed unary POST request") {
    val uri         = serviceUri / UnaryServicePrefix / "sayHello"
    val request     = Request[IO](Method.POST, uri)
    val requestBody = "{"
    val response =
      BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
    response.attempt.assertEquals(Left(UnexpectedStatus(Status.BadRequest, Method.POST, uri)))
  }

  test("REST Server should return a 400 Bad Request for a malformed streaming POST request") {
    val uri         = serviceUri / Fs2ServicePrefix / "sayHellos"
    val request     = Request[IO](Method.POST, uri)
    val requestBody = "{"
    val response =
      BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
    response.attempt.assertEquals(Left(UnexpectedStatus(Status.BadRequest, Method.POST, uri)))
  }

  val unaryServiceClient: UnaryGreeterRestClient[IO] =
    new UnaryGreeterRestClient[IO](serviceUri / UnaryServicePrefix)
  val fs2ServiceClient: Fs2GreeterRestClient[IO] =
    new Fs2GreeterRestClient[IO](serviceUri / Fs2ServicePrefix)

  test("REST Service should serve a GET request") {
    val response = BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.getHello()(_))
    response.assertEquals(HelloResponse("hey"))
  }

  test("REST Service should serve a unary POST request") {
    val request = HelloRequest("hey")
    val response =
      BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
    response.assertEquals(HelloResponse("hey"))
  }

  test("REST Service should handle a raised gRPC exception in a unary POST request") {
    val request = HelloRequest("SRE")
    val response =
      BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
    response.attempt.assertEquals(
      Left(ResponseError(Status.BadRequest, Some("INVALID_ARGUMENT: SRE")))
    )
  }

  test("REST Service should handle a raised non-gRPC exception in a unary POST request") {
    val request = HelloRequest("RTE")
    val response =
      BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
    response.attempt.assertEquals(Left(ResponseError(Status.InternalServerError, Some("RTE"))))
  }

  test("REST Service should handle a thrown exception in a unary POST request") {
    val request = HelloRequest("TR")
    val response =
      BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
    response.attempt.assertEquals(Left(ResponseError(Status.InternalServerError)))
  }

  test("REST Service should serve a POST request with fs2 streaming request") {
    val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
    val response =
      BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
    response.assertEquals(HelloResponse("hey, there"))
  }

  test("REST Service should serve a POST request with empty fs2 streaming request") {
    val requests = Stream.empty
    val response =
      BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
    response.assertEquals(HelloResponse(""))
  }

  test("REST Service should serve a POST request with fs2 streaming response") {
    val request = HelloRequest("hey")
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
    responses.compile.toList
      .assertEquals(List(HelloResponse("hey"), HelloResponse("hey")))
  }

  test("REST Service should handle errors with fs2 streaming response") {
    val request = HelloRequest("")
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
    interceptMessageIO[UnexpectedError]("java.lang.IllegalArgumentException: empty greeting")(
      responses.compile.toList
    )
  }

  test("REST Service should serve a POST request with bidirectional fs2 streaming") {
    val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
    responses.compile.toList
      .assertEquals(List(HelloResponse("hey"), HelloResponse("there")))
  }

  test("REST Service should serve an empty POST request with bidirectional fs2 streaming") {
    val requests = Stream.empty
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
    responses.compile.toList.assertEquals(Nil)
  }
}
