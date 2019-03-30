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
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import higherkindness.mu.http.implicits._
import higherkindness.mu.rpc.protocol.Empty
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UnaryGreeterRestService[F[_]: Sync](
    implicit handler: UnaryGreeter[F],
    decoderHelloRequest: io.circe.Decoder[HelloRequest],
    encoderHelloResponse: io.circe.Encoder[HelloResponse],
    encoderEmptyResponse: io.circe.Encoder[EmptyResponse])
    extends Http4sDsl[F] {

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

  def service: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / "getHello" => Ok(handler.getHello(Empty).map(_.asJson))

    case OPTIONS -> Root / "optionsHello" => Ok(handler.optionsHello(Empty).map(_.asJson))

    case HEAD -> Root / "headHello" =>
      handler.headHello(Empty).flatMap(r => NoContent(r.asHeader))

    case TRACE -> Root / "traceHello" =>
      handler.traceHello(Empty).flatMap(r => NoContent(r.asHeader))

    case CONNECT -> Root / "connectHello" =>
      handler.connectHello(Empty).flatMap(a => Ok(a.asJson))

    case msg @ PUT -> Root / "putHello" =>
      for {
        request  <- msg.as[HelloRequest]
        put      <- handler.putHello(request)
        response <- Accepted(put.asHeader)
      } yield response

    case msg @ PATCH -> Root / "patchHello" =>
      for {
        request  <- msg.as[HelloRequest]
        patch    <- handler.patchHello(request)
        response <- Accepted(patch.asHeader)
      } yield response

    case msg @ DELETE -> Root / "deleteHello" =>
      for {
        request  <- msg.as[HelloRequest]
        response <- Ok(handler.deleteHello(request).map(_.asJson)).adaptErrors
      } yield response

    case msg @ POST -> Root / "sayHello" =>
      for {
        request  <- msg.as[HelloRequest]
        response <- Ok(handler.sayHello(request).map(_.asJson)).adaptErrors
      } yield response

  }
}

class Fs2GreeterRestService[F[_]: Sync](
    implicit handler: Fs2Greeter[F],
    decoderHelloRequest: io.circe.Decoder[HelloRequest],
    encoderHelloResponse: io.circe.Encoder[HelloResponse])
    extends Http4sDsl[F] {

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

  def service: HttpRoutes[F] = HttpRoutes.of[F] {

    case msg @ POST -> Root / "sayHellos" =>
      val requests = msg.asStream[HelloRequest]
      Ok(handler.sayHellos(requests).map(_.asJson))

    case msg @ POST -> Root / "sayHelloAll" =>
      for {
        request   <- msg.as[HelloRequest]
        responses <- Ok(handler.sayHelloAll(request).asJsonEither)
      } yield responses

    case msg @ POST -> Root / "sayHellosAll" =>
      val requests = msg.asStream[HelloRequest]
      Ok(handler.sayHellosAll(requests).asJsonEither)
  }
}
