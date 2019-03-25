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
package client

import cats.effect.{ContextShift, Effect}
import io.grpc.stub.ClientCalls
import io.grpc.{CallOptions, Channel, MethodDescriptor}

object unaryCalls {

  def unary[F[_]: Effect: ContextShift, Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): F[Res] =
    listenableFuture2Async[F, Res](
      ClientCalls
        .futureUnaryCall(channel.newCall(descriptor, options), request))
}
