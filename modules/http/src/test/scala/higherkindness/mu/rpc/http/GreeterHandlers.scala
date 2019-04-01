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

import cats.{Applicative, MonadError}
import cats.effect._

class UnaryGreeterHandler[F[_]: Applicative](implicit F: MonadError[F, Throwable])
    extends UnaryGreeter[F] {

  import cats.syntax.applicative._
  import higherkindness.mu.rpc.protocol.Empty
  import io.grpc.Status._

  def getHello(request: Empty.type): F[HelloResponse] = HelloResponse("hey").pure

  def sayHello(request: HelloRequest): F[HelloResponse] = request.hello match {
    case "SE"  => F.raiseError(INVALID_ARGUMENT.withDescription("SE").asException)
    case "SRE" => F.raiseError(INVALID_ARGUMENT.withDescription("SRE").asRuntimeException)
    case "RTE" => F.raiseError(new IllegalArgumentException("RTE"))
    case "TR"  => throw new IllegalArgumentException("Thrown")
    case other => HelloResponse(other).pure
  }

}

class Fs2GreeterHandler[F[_]: Sync] extends Fs2Greeter[F] {

  import fs2.Stream

  def sayHellos(requests: Stream[F, HelloRequest]): F[HelloResponse] =
    requests.compile.fold(HelloResponse("")) {
      case (response, request) =>
        HelloResponse(
          if (response.hello.isEmpty) request.hello else s"${response.hello}, ${request.hello}")
    }

  def sayHelloAll(request: HelloRequest): Stream[F, HelloResponse] =
    if (request.hello.isEmpty) Stream.raiseError(new IllegalArgumentException("empty greeting"))
    else Stream(HelloResponse(request.hello), HelloResponse(request.hello))

  def sayHellosAll(requests: Stream[F, HelloRequest]): Stream[F, HelloResponse] =
    requests.map(request => HelloResponse(request.hello))
}