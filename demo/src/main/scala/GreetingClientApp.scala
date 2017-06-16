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
import io.grpc.stub.StreamObserver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

object GreetingClientApp {

  private val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build

  def main(args: Array[String]): Unit = {

    val request = MessageRequest("Freestyle")

    val asyncHelloClient: GreeterStub = GreeterGrpc.stub(channel)

    // Unary RPCs where the client sends a single request to the server and
    // gets a single response back, just like a normal function call:

    val response = for {
      hi  <- asyncHelloClient.sayHello(request)
      bye <- asyncHelloClient.sayGoodbye(request)
    } yield (hi.message, bye.message)

    println("")
    println(s"Received -> ${Await.result(response, Duration.Inf)}")
    println("")

    // Server streaming RPCs where the client sends a request to the server and
    // gets a stream to read a sequence of messages back. The client reads from
    // the returned stream until there are no more messages.

    val streamingCompleted = Promise[Unit]()
    val lotOfRepliesObserver = new StreamObserver[MessageReply] {

      override def onError(t: Throwable): Unit = println(s"Streaming failure: ${t.getMessage}")

      override def onCompleted(): Unit = {
        println("Lot of Replies streaming completed")
        streamingCompleted.success((): Unit)
      }

      override def onNext(value: MessageReply): Unit = println(s"Received by streaming -> $value")
    }

    asyncHelloClient.lotsOfReplies(request, lotOfRepliesObserver)

    Await.ready(streamingCompleted.future, Duration.Inf)
    (): Unit
  }
}
