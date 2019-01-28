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

import cats.ApplicativeError
import cats.effect._
import cats.implicits._
import fs2.interop.reactivestreams._
import fs2.{RaiseThrowable, Stream}
import io.grpc.Status.Code._
import jawn.ParseException
import io.circe._
import io.circe.generic.auto._
import io.circe.jawn.CirceSupportParser.facade
import io.circe.syntax._
import io.grpc.{Status => _, _}
import jawnfs2._
import monix.execution._
import monix.reactive.Observable
import org.http4s._
import org.http4s.dsl.Http4sDsl
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace

object Utils {

  private[http] implicit class MessageOps[F[_]](val message: Message[F]) extends AnyVal {

    def jsonBodyAsStream[A](
        implicit decoder: Decoder[A],
        F: ApplicativeError[F, Throwable]): Stream[F, A] =
      message.body.chunks.parseJsonStream.map(_.as[A]).rethrow
  }

  private[http] implicit class RequestOps[F[_]](val request: Request[F]) {

    def asStream[A](implicit decoder: Decoder[A], F: ApplicativeError[F, Throwable]): Stream[F, A] =
      request
        .jsonBodyAsStream[A]
        .adaptError { // mimic behavior of MessageOps.as[T] in handling of parsing errors
          case ex: ParseException =>
            MalformedMessageBodyFailure(ex.getMessage, Some(ex)) // will return 400 instead of 500
        }
  }

  private[http] implicit class ResponseOps[F[_]](val response: Response[F]) {

    implicit private val throwableDecoder: Decoder[Throwable] =
      Decoder.decodeTuple2[String, String].map {
        case (cls, msg) =>
          Class
            .forName(cls)
            .getConstructor(classOf[String])
            .newInstance(msg)
            .asInstanceOf[Throwable]
      }

    def asStream[A](
        implicit decoder: Decoder[A],
        F: ApplicativeError[F, Throwable],
        R: RaiseThrowable[F]): Stream[F, A] =
      if (response.status.code != 200) Stream.raiseError(ResponseError(response.status))
      else response.jsonBodyAsStream[Either[Throwable, A]].rethrow
  }

  private[http] implicit class Fs2StreamOps[F[_], A](stream: Stream[F, A]) {

    implicit private val throwableEncoder: Encoder[Throwable] = new Encoder[Throwable] {
      def apply(ex: Throwable): Json = (ex.getClass.getName, ex.getMessage).asJson
    }

    def asJsonEither(implicit encoder: Encoder[A]): Stream[F, Json] = stream.attempt.map(_.asJson)

    def toObservable(implicit F: ConcurrentEffect[F], ec: ExecutionContext): Observable[A] =
      Observable.fromReactivePublisher(stream.toUnicastPublisher)
  }

  private[http] implicit class MonixStreamOps[A](val stream: Observable[A]) extends AnyVal {

    def toFs2Stream[F[_]](implicit F: ConcurrentEffect[F], sc: Scheduler): Stream[F, A] =
      stream.toReactivePublisher.toStream[F]()
  }

  private[http] implicit class FResponseOps[F[_]: Sync](response: F[Response[F]])
      extends Http4sDsl[F] {

    def adaptErrors: F[Response[F]] = response.handleErrorWith {
      case se: StatusException         => errorFromStatus(se.getStatus, se.getMessage)
      case sre: StatusRuntimeException => errorFromStatus(sre.getStatus, sre.getMessage)
      case other: Throwable            => InternalServerError(other.getMessage)
    }

    private def errorFromStatus(status: io.grpc.Status, message: String): F[Response[F]] =
      status.getCode match {
        case INVALID_ARGUMENT  => BadRequest(message)
        case UNAUTHENTICATED   => BadRequest(message)
        case PERMISSION_DENIED => Forbidden(message)
        case NOT_FOUND         => NotFound(message)
        case UNAVAILABLE       => ServiceUnavailable(message)
        case _                 => InternalServerError(message)
      }
  }

  private[http] def handleResponseError[F[_]: Sync](errorResponse: Response[F]): F[Throwable] =
    errorResponse.bodyAsText.compile.foldMonoid.map(body =>
      ResponseError(errorResponse.status, Some(body).filter(_.nonEmpty)))
}

final case class ResponseError(status: Status, msg: Option[String] = None)
    extends RuntimeException(status + msg.fold("")(": " + _))
    with NoStackTrace
