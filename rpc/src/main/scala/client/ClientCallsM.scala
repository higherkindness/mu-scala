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
package client

import freestyle.free
import io.grpc._
import io.grpc.stub.StreamObserver

@free
trait ClientCallsM {

  def asyncUnaryCall[I, O](call: ClientCall[I, O], param: I, observer: StreamObserver[O]): FS[Unit]

  def asyncServerStreamingCall[I, O](
      call: ClientCall[I, O],
      param: I,
      responseObserver: StreamObserver[O]): FS[Unit]

  def asyncClientStreamingCall[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): FS[StreamObserver[I]]

  def asyncBidiStreamingCall[I, O](
      call: ClientCall[I, O],
      responseObserver: StreamObserver[O]): FS[StreamObserver[I]]

  def blockingUnaryCall[I, O](call: ClientCall[I, O], param: I): FS[O]

  def blockingUnaryCallChannel[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): FS[O]

  def blockingServerStreamingCall[I, O](call: ClientCall[I, O], param: I): FS[Iterator[O]]

  def blockingServerStreamingCallChannel[I, O](
      channel: Channel,
      method: MethodDescriptor[I, O],
      callOptions: CallOptions,
      param: I): FS[Iterator[O]]

  def futureUnaryCall[I, O](call: ClientCall[I, O], param: I): FS[O]

}
