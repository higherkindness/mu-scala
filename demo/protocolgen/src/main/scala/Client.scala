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
package demo
package protocolgen

import freestyle.rpc.demo.protocolgen.protocols._
import _root_.io.grpc._

object Client {

  trait GreetingServiceBlockingClient {
    def sayHello(request: MessageRequest): MessageReply
  }

  class GreetingServiceStub(channel: Channel, options: CallOptions = CallOptions.DEFAULT)
      extends stub.AbstractStub[GreetingServiceStub](channel, options) {

    def sayHello(request: MessageRequest): scala.concurrent.Future[MessageReply] =
      com.trueaccord.scalapb.grpc.Grpc.guavaFuture2ScalaFuture(
        stub.ClientCalls
          .futureUnaryCall(
            channel.newCall(GreetingService.sayHelloMethodDescriptor, options),
            request))

    override def build(channel: Channel, options: CallOptions): GreetingServiceStub =
      new GreetingServiceStub(channel, options)
  }

  def clientStub(channel: Channel): GreetingServiceStub = new GreetingServiceStub(channel)

}
