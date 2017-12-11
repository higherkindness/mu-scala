/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle
package rpc
package internal
package client

import freestyle.free.FSHandler
import freestyle.async.AsyncContext
import freestyle.free.asyncGuava.implicits._
import io.grpc.{CallOptions, Channel, MethodDescriptor}
import io.grpc.stub.{ClientCalls, StreamObserver}
import monix.eval.Task
import monix.reactive.Observable
import org.reactivestreams._

import scala.concurrent.ExecutionContext

object calls {

  import internal.converters._

  def unary[M[_], Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit AC: AsyncContext[M], E: ExecutionContext): M[Res] =
    listenableFuture2Async.apply(
      ClientCalls
        .futureUnaryCall(channel.newCall(descriptor, options), request))

  def serverStreaming[Req, Res](
      request: Req,
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    Observable
      .fromReactivePublisher(new Publisher[Res] {
        override def subscribe(s: Subscriber[_ >: Res]): Unit = {
          val subscriber: Subscriber[Res] = s.asInstanceOf[Subscriber[Res]]
          ClientCalls.asyncServerStreamingCall(
            channel.newCall[Req, Res](descriptor, options),
            request,
            subscriber
          )
        }
      })

  def clientStreaming[M[_], Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions)(implicit H: FSHandler[Task, M]): M[Res] =
    H(
      input
        .liftByOperator(
          StreamObserver2MonixOperator(
            (outputObserver: StreamObserver[Res]) =>
              ClientCalls.asyncClientStreamingCall(
                channel.newCall(descriptor, options),
                outputObserver
            )
          )
        )
        .firstL)

  def bidiStreaming[Req, Res](
      input: Observable[Req],
      descriptor: MethodDescriptor[Req, Res],
      channel: Channel,
      options: CallOptions): Observable[Res] =
    input.liftByOperator(
      StreamObserver2MonixOperator(
        (outputObserver: StreamObserver[Res]) =>
          ClientCalls.asyncBidiStreamingCall(
            channel.newCall(descriptor, options),
            outputObserver
        ))
    )
}
