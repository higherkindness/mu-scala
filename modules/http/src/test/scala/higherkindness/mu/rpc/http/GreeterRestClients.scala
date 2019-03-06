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

import cats.effect._
import fs2.Stream
import fs2.interop.reactivestreams._
import io.circe.generic.auto._
import io.circe.syntax._
import higherkindness.mu.http.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._

class UnaryGreeterRestClient[F[_]: Sync](uri: Uri) {

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def getHello()(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.GET, uri / "getHello")
    client.expectOr[HelloResponse](request)(handleResponseError)
  }

  def sayHello(arg: HelloRequest)(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHello")
    client.expectOr[HelloResponse](request.withEntity(arg.asJson))(handleResponseError)
  }

}

class Fs2GreeterRestClient[F[_]: Sync](uri: Uri) {

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def sayHellos(arg: Stream[F, HelloRequest])(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellos")
    client.expectOr[HelloResponse](request.withEntity(arg.map(_.asJson)))(handleResponseError)
  }

  def sayHelloAll(arg: HelloRequest)(implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHelloAll")
    client.stream(request.withEntity(arg.asJson)).flatMap(_.asStream[HelloResponse])
  }

  def sayHellosAll(arg: Stream[F, HelloRequest])(
      implicit client: Client[F]): Stream[F, HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellosAll")
    client.stream(request.withEntity(arg.map(_.asJson))).flatMap(_.asStream[HelloResponse])
  }

}

class MonixGreeterRestClient[F[_]: ConcurrentEffect](uri: Uri)(
    implicit sc: monix.execution.Scheduler) {

  import monix.reactive.Observable
  import higherkindness.mu.http.implicits._

  private implicit val responseDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]

  def sayHellos(arg: Observable[HelloRequest])(implicit client: Client[F]): F[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellos")
    client.expectOr[HelloResponse](
      request.withEntity(arg.toReactivePublisher.toStream.map(_.asJson)))(handleResponseError)
  }

  def sayHelloAll(arg: HelloRequest)(implicit client: Client[F]): Observable[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHelloAll")
    Observable.fromReactivePublisher(
      client
        .stream(request.withEntity(arg.asJson))
        .flatMap(_.asStream[HelloResponse])
        .toUnicastPublisher)
  }

  def sayHellosAll(arg: Observable[HelloRequest])(
      implicit client: Client[F]): Observable[HelloResponse] = {
    val request = Request[F](Method.POST, uri / "sayHellosAll")
    Observable.fromReactivePublisher(
      client
        .stream(request.withEntity(arg.toReactivePublisher.toStream.map(_.asJson)))
        .flatMap(_.asStream[HelloResponse])
        .toUnicastPublisher)
  }

}
