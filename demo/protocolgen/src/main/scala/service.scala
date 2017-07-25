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
package demo
package protocolgen

import freestyle.rpc.protocol._
import monix.reactive.Observable

@option(name = "java_package", value = "com.example.foo", quote = true)
@option(name = "java_multiple_files", value = "true", quote = false)
@option(name = "java_outer_classname", value = "Ponycopter", quote = true)
object protocols {

  @message
  case class MessageRequest(name: String, n: Option[Int])

  @message
  case class MessageReply(name: String, n: List[Int])

  @free
  @service
  @debug
  trait GreetingService {

    @rpc def sayHello(msg: MessageRequest): FS[MessageReply]

    @rpc
    @stream[ResponseStreaming.type]
    def lotsOfReplies(msg: MessageRequest): FS[Observable[MessageReply]]

    @rpc
    @stream[RequestStreaming.type]
    def lotsOfGreetings(msg: Observable[MessageRequest]): FS[MessageReply]

    @rpc
    @stream[BidirectionalStreaming.type]
    def bidiHello(msg: Observable[MessageRequest]): FS[Observable[MessageReply]]
  }

}
