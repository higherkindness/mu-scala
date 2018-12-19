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

package higherkindness.mu.rpc
package internal

import io.grpc.{CallOptions, Channel, MethodDescriptor}
import io.grpc.stub.ClientCalls
import monix.execution.rstreams.Subscription
import org.reactivestreams.{Publisher, Subscriber}

package object client {

  import higherkindness.mu.rpc.internal.converters._

  private[client] def createPublisher[Res, Req](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions) = {
    new Publisher[Res] {
      override def subscribe(s: Subscriber[_ >: Res]): Unit = {
        val subscriber: Subscriber[Res] = s.asInstanceOf[Subscriber[Res]]
        s.onSubscribe(Subscription.empty)
        ClientCalls.asyncServerStreamingCall(
          channel.newCall[Req, Res](descriptor, options),
          request,
          subscriber.toStreamObserver
        )
      }
    }
  }
}
