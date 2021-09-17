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
import higherkindness.mu.http.protocol.{HttpServer, RouteMap}
import io.circe.generic.auto._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server._

abstract class GreeterDerivedRestTests extends CatsEffectSuite {

  val host            = "localhost"
  val port            = 8080
  val serviceUri: Uri = Uri.unsafeFromString(s"http://$host:$port")

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]
  implicit val fs2HandlerIO   = new Fs2GreeterHandler[IO]

  val unaryRoute: RouteMap[IO] = UnaryGreeter.route[IO]
  val fs2Route: RouteMap[IO]   = Fs2Greeter.route[IO]

  val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, unaryRoute, fs2Route)

  val serverFixture = ResourceSuiteLocalFixture(
    "blaze-server",
    Resource.make(server.resource.use(_ => IO.never).start)(_.cancel)
  )

  override def munitFixtures = List(serverFixture)

  val unaryClient: UnaryGreeter.HttpClient[IO] = UnaryGreeter.httpClient[IO](serviceUri)
  val fs2Client: Fs2Greeter.HttpClient[IO]     = Fs2Greeter.httpClient[IO](serviceUri)

  test("REST Service should serve a GET request") {
    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(unaryClient.getHello(_))
    IO(serverFixture()) *>
      response.assertEquals(HelloResponse("hey"))
  }

  test("REST Service should serve a unary POST request") {
    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("hey"))(_))
    IO(serverFixture()) *>
      response.assertEquals(HelloResponse("hey"))
  }

  test("REST Service should handle a raised gRPC exception in a unary POST request") {
    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("SRE"))(_))
    IO(serverFixture()) *>
      response.attempt.assertEquals(
        Left(ResponseError(Status.BadRequest, Some("INVALID_ARGUMENT: SRE")))
      )
  }

  test("REST Service should handle a raised non-gRPC exception in a unary POST request") {
    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("RTE"))(_))
    IO(serverFixture()) *>
      response.attempt.assertEquals(Left(ResponseError(Status.InternalServerError, Some("RTE"))))
  }

  test("REST Service should handle a thrown exception in a unary POST request") {
    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(unaryClient.sayHello(HelloRequest("TR"))(_))
    IO(serverFixture()) *>
      response.attempt.assertEquals(Left(ResponseError(Status.InternalServerError)))
  }

  test("REST Service should serve a POST request with fs2 streaming request") {

    val requests = Stream(HelloRequest("hey"), HelloRequest("there"))

    val response: IO[HelloResponse] =
      BlazeClientBuilder[IO](ec).resource.use(fs2Client.sayHellos(requests)(_))
    IO(serverFixture()) *>
      response.assertEquals(HelloResponse("hey, there"))
  }

  test("REST Service should serve a POST request with empty fs2 streaming request") {
    val requests = Stream.empty
    val response =
      BlazeClientBuilder[IO](ec).resource.use(fs2Client.sayHellos(requests)(_))
    IO(serverFixture()) *>
      response.assertEquals(HelloResponse(""))
  }

  test("REST Service should serve a POST request with fs2 streaming response") {
    val request = HelloRequest("hey")
    val responses =
      BlazeClientBuilder[IO](ec).stream
        .evalMap(client => fs2Client.sayHelloAll(request)(client))
        .flatten
    IO(serverFixture()) *>
      responses.compile.toList
        .assertEquals(List(HelloResponse("hey"), HelloResponse("hey")))
  }

  test("REST Service should handle errors with fs2 streaming response") {
    val request = HelloRequest("")
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(client =>
        Stream.force(fs2Client.sayHelloAll(request)(client))
      )
    IO(serverFixture()) *>
      interceptMessageIO[UnexpectedError]("java.lang.IllegalArgumentException: empty greeting")(
        responses.compile.toList
      )
  }

  test("REST Service should serve a POST request with bidirectional fs2 streaming") {
    val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(client =>
        Stream.force(fs2Client.sayHellosAll(requests)(client))
      )
    IO(serverFixture()) *>
      responses.compile.toList
        .assertEquals(List(HelloResponse("hey"), HelloResponse("there")))
  }

  test("REST Service should serve an empty POST request with bidirectional fs2 streaming") {
    val requests = Stream.empty
    val responses =
      BlazeClientBuilder[IO](ec).stream.flatMap(client =>
        Stream.force(fs2Client.sayHellosAll(requests)(client))
      )
    IO(serverFixture()) *>
      responses.compile.toList.assertEquals(Nil)
  }

}
