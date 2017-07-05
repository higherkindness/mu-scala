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

package freestyle.rpc.demo
package greeting.client

import freestyle._
import freestyle.rpc.client._
import freestyle.rpc.demo.greeting._
import io.grpc.CallOptions
import io.grpc.stub.StreamObserver

@module
trait GreetingClientM {

  val clientCallsM: ClientCallsM
  val channelOps: ChannelM

  def sayHello(
      request: MessageRequest,
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[MessageReply] =
    for {
      call     <- channelOps.newCall(GreeterGrpc.METHOD_SAY_HELLO, options)
      response <- clientCallsM.asyncM(call, request)
    } yield response

  def sayGoodbye(
      request: MessageRequest,
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[MessageReply] =
    for {
      call     <- channelOps.newCall(GreeterGrpc.METHOD_SAY_GOODBYE, options)
      response <- clientCallsM.asyncM(call, request)
    } yield response

  def lotsOfReplies(
      request: MessageRequest,
      responseObserver: StreamObserver[MessageReply],
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[Unit] =
    for {
      call     <- channelOps.newCall(GreeterGrpc.METHOD_LOTS_OF_REPLIES, options)
      response <- clientCallsM.asyncStreamServer(call, request, responseObserver)
    } yield response

  def lotsOfGreetings(
      responseObserver: StreamObserver[MessageReply],
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[StreamObserver[MessageRequest]] =
    for {
      call     <- channelOps.newCall(GreeterGrpc.METHOD_LOTS_OF_GREETINGS, options)
      response <- clientCallsM.asyncStreamClient(call, responseObserver)
    } yield response

  def bidiHello(
      responseObserver: StreamObserver[MessageReply],
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[StreamObserver[MessageRequest]] =
    for {
      call     <- channelOps.newCall(GreeterGrpc.METHOD_BIDI_HELLO, options)
      response <- clientCallsM.asyncStreamBidi(call, responseObserver)
    } yield response

}
