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

import io.grpc.ManagedChannelBuilder
import freestyle.rpc.demo.greeting.GreeterGrpc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object GreetingClientApp {

  private val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build

  def main(args: Array[String]): Unit = {

    val request = MessageRequest("Freestyle")

    val asyncHelloClient: GreeterStub = GreeterGrpc.stub(channel)

    val response = for {
      hi  <- asyncHelloClient.sayHello(request)
      bye <- asyncHelloClient.sayGoodbye(request)
    } yield (hi.message, bye.message)

    println("")
    println(s"Received -> ${Await.result(response, Duration.Inf)}")
    println("")
  }
}
