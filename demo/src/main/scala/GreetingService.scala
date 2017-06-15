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
package greeting

import scala.concurrent.Future

class GreetingService extends GreeterGrpc.Greeter {

  def sayHello(request: MessageRequest): Future[MessageReply] = {
    println(s"Hi message received from ${request.name}")
    Future.successful(MessageReply(s"Hello ${request.name} from HelloService!"))
  }

  def sayGoodbye(request: MessageRequest): Future[MessageReply] = {
    println(s"Goodbye message received from ${request.name}")
    Future.successful(MessageReply(s"See you soon ${request.name}!"))
  }
}
