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

import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import freestyle.rpc.protocol.Empty
import fs2.Stream
import jawn.ParseException
import io.circe._
import io.circe.generic.auto._
import io.circe.jawn.CirceSupportParser.facade
import io.circe.syntax._
import jawnfs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class GreeterRestService[F[_]: Sync](handler: Greeter[F]) extends Http4sDsl[F] {

  import freestyle.rpc.http.GreeterRestService._

  private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

  def service: HttpService[F] = HttpService[F] {

    case GET -> Root / "getHello" => Ok(handler.getHello(Empty).map(_.asJson))

    case msg @ POST -> Root / "sayHello" =>
      for {
        request  <- msg.as[HelloRequest]
        response <- Ok(handler.sayHello(request).map(_.asJson))
      } yield response

    case msg @ POST -> Root / "sayHellos" =>
      for {
        requests <- msg.asStream[HelloRequest]
        response <- Ok(handler.sayHellos(requests).map(_.asJson))
      } yield response

    case msg @ POST -> Root / "sayHelloAll" =>
      for {
        request   <- msg.as[HelloRequest]
        responses <- Ok(handler.sayHelloAll(request).map(_.asJson))
      } yield responses

    case msg @ POST -> Root / "sayHellosAll" =>
      for {
        requests  <- msg.asStream[HelloRequest]
        responses <- Ok(handler.sayHellosAll(requests).map(_.asJson))
      } yield responses

  }
}

object GreeterRestService {

  implicit class RequestOps[F[_]: Sync](request: Request[F]) {

    def asStream[A](implicit decoder: Decoder[A]): F[Stream[F, A]] =
      request.body.chunks.parseJsonStream
        .map(_.as[A])
        .handleErrorWith {
          case ex: ParseException =>
            throw MalformedMessageBodyFailure(ex.getMessage, Some(ex)) // will return 400 instead of 500
        }
        .rethrow
        .pure
  }
}
