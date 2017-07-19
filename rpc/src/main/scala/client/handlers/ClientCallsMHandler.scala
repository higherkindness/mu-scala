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

package freestyle.rpc
package client.handlers

import freestyle.Capture
import freestyle.async.AsyncContext
import freestyle.rpc.client.ClientCallsM
import io.grpc.{CallOptions, Channel, ClientCall, MethodDescriptor}
import io.grpc.stub.{ClientCalls, StreamObserver}
import freestyle.rpc.client.implicits._

import scala.collection.JavaConverters._

class ClientCallsMHandler[M[_]](implicit C: Capture[M], AC: AsyncContext[M])
    extends ClientCallsM.Handler[M] {

  def async[I, O](call: ClientCall[I, O], param: I, observer: StreamObserver[O]): M[Unit] =
    C.capture(ClientCalls.asyncUnaryCall(call, param, observer))

  def asyncStreamServer[I, O](
      call: ClientCall[I, O],
      param: I,
      responseObserver: StreamObserver[O]): M[Unit] =
    C.capture(ClientCalls.asyncServerStreamingCall(call, param, responseObserver))

  def asyncStreamClient[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): M[StreamObserver[I]] =
    C.capture(ClientCalls.asyncClientStreamingCall(call, responseObserver))

  def asyncStreamBidi[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): M[StreamObserver[I]] =
    C.capture(ClientCalls.asyncBidiStreamingCall(call, responseObserver))

  def sync[I, O](call: ClientCall[I, O], param: I): M[O] =
    C.capture(ClientCalls.blockingUnaryCall(call, param))

  def syncC[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): M[O] =
    C.capture(ClientCalls.blockingUnaryCall(channel, method, callOptions, param))

  def syncStreamServer[I, O](call: ClientCall[I, O], param: I): M[Iterator[O]] =
    C.capture(ClientCalls.blockingServerStreamingCall(call, param).asScala)

  def syncStreamServerC[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): M[Iterator[O]] =
    C.capture(ClientCalls.blockingServerStreamingCall(channel, method, callOptions, param).asScala)

  import client.implicits._
  def asyncM[I, O](call: ClientCall[I, O], param: I): M[O] =
    ClientCalls.futureUnaryCall(call, param)
}
