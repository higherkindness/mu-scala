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

package higherkindness.mu.rpc
package internal
package server

import fs2.Stream
import cats.effect.ConcurrentEffect
import io.grpc.ServerCallHandler
import org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler

import scala.concurrent.ExecutionContext

object fs2Calls {

  def unaryMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Req => F[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].unaryToUnaryCall[Req, Res]((req, _) => f(req))

  def clientStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Stream[F, Req] => F[Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].streamingToUnaryCall[Req, Res]((stream, _) => f(stream))

  def serverStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Req => Stream[F, Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].unaryToStreamingCall[Req, Res]((req, _) => f(req))

  def bidiStreamingMethod[F[_]: ConcurrentEffect, Req, Res](
      f: Stream[F, Req] => Stream[F, Res],
      maybeCompression: Option[String])(
      implicit EC: ExecutionContext): ServerCallHandler[Req, Res] =
    Fs2ServerCallHandler[F].streamingToStreamingCall[Req, Res]((stream, _) => f(stream))

}
