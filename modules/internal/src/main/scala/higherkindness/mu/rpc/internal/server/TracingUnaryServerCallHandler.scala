/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.internal.server

import higherkindness.mu.rpc.protocol.CompressionType
import cats.data.Kleisli
import cats.effect.Effect
import io.grpc._
import io.grpc.ServerCall.Listener
import io.grpc.stub.ServerCalls
import natchez.{EntryPoint, Span}

class TracingUnaryServerCallHandler[F[_]: Effect, Req, Res](
    f: Req => Kleisli[F, Span[F], Res],
    methodDescriptor: MethodDescriptor[Req, Res],
    entrypoint: EntryPoint[F],
    compressionType: CompressionType
) extends ServerCallHandler[Req, Res] {

  def startCall(
      call: ServerCall[Req, Res],
      metadata: Metadata
  ): Listener[Req] = {
    val kernel       = extractTracingKernel(metadata)
    val spanResource = entrypoint.continueOrElseRoot(methodDescriptor.getFullMethodName(), kernel)

    val unaryMethod = unaryCalls.unaryMethod[F, Req, Res](
      req => spanResource.use(span => f(req).run(span)),
      compressionType
    )

    ServerCalls.asyncUnaryCall(unaryMethod).startCall(call, metadata)
  }

}
