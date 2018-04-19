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
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.UnexpectedStatus
import org.http4s.client.blaze.Http1Client
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.scalatest._

class GreeterRestTests extends RpcBaseTestSuite with BeforeAndAfter {

  val Hostname = "localhost"
  val Port     = 8080

  val serviceUri: Uri          = Uri.unsafeFromString(s"http://$Hostname:$Port")
  val service: HttpService[IO] = new GreeterRestService[IO](new GreeterHandler[IO]).service
  val server: BlazeBuilder[IO] =
    BlazeBuilder[IO].bindHttp(Port, Hostname).mountService(service, "/")

  var serverTask: Server[IO] = _ // sorry
  before(serverTask = server.start.unsafeRunSync())
  after(serverTask.shutdownNow())

  "REST Server" should {

    "serve a GET request" in {
      val request = Request[IO](Method.GET, serviceUri / "getHello")
      val response = (for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request)
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("hey").asJson
    }

    "serve a POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / "sayHello")
      val requestBody = HelloRequest("hey").asJson
      val response = (for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("hey").asJson
    }

    "return a 400 Bad Request for a malformed unary POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / "sayHello")
      val requestBody = "hey"
      val responseError = the[UnexpectedStatus] thrownBy (for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response).unsafeRunSync()
      responseError.status.code shouldBe 400
    }

    "return a 400 Bad Request for a malformed streaming POST request" in {
      val request     = Request[IO](Method.POST, serviceUri / "sayHellos")
      val requestBody = "{"
      val responseError = the[UnexpectedStatus] thrownBy (for {
        client   <- Http1Client[IO]()
        response <- client.expect[Json](request.withBody(requestBody))
      } yield response).unsafeRunSync()
      responseError.status.code shouldBe 400
    }

  }

  val serviceClient: GreeterRestClient[IO] = new GreeterRestClient[IO](serviceUri)

  "REST Service" should {

    "serve a GET request" in {
      val response = (for {
        client   <- Http1Client[IO]()
        response <- serviceClient.getHello()(client)
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("hey")
    }

    "serve a unary POST request" in {
      val request = HelloRequest("hey")
      val response = (for {
        client   <- Http1Client[IO]()
        response <- serviceClient.sayHello(request)(client)
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("hey")
    }

    "serve a POST request with fs2 streaming request" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val response = (for {
        client   <- Http1Client[IO]()
        response <- serviceClient.sayHellos(requests)(client)
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("hey, there")
    }

    "serve a POST request with empty fs2 streaming request" in {
      val requests = Stream.empty
      val response = (for {
        client   <- Http1Client[IO]()
        response <- serviceClient.sayHellos(requests)(client)
      } yield response).unsafeRunSync()
      response shouldBe HelloResponse("")
    }

    "serve a POST request with fs2 streaming response" in {
      val request = HelloRequest("hey")
      val responses = (for {
        client   <- Http1Client.stream[IO]()
        response <- serviceClient.sayHelloAll(request)(client)
      } yield response).compile.toList.unsafeRunSync()
      responses shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
    }

    "serve a POST request with bidirectional fs2 streaming" in {
      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
      val responses = (for {
        client   <- Http1Client.stream[IO]()
        response <- serviceClient.sayHellosAll(requests)(client)
      } yield response).compile.toList.unsafeRunSync()
      responses shouldBe List(HelloResponse("hey"), HelloResponse("there"))
    }
  }

}
