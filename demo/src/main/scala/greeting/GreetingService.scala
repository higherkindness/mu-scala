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

import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import freestyle.rpc.demo.greeting.{GreeterGrpc, MessageReply, MessageRequest}
import io.grpc.stub.StreamObserver

import scala.concurrent.Future

class GreetingService extends GreeterGrpc.Greeter {

  val numberOfReplies    = 10
  val initialDelay: Long = 0l
  val interval: Long     = 500l

  // rpc SayHello (MessageRequest) returns (MessageReply) {}
  def sayHello(request: MessageRequest): Future[MessageReply] = {
    println(s"Hi message received from ${request.name}")
    Future.successful(MessageReply(s"Hello ${request.name} from HelloService!"))
  }

  // rpc SayGoodbye (MessageRequest) returns (MessageReply) {}
  def sayGoodbye(request: MessageRequest): Future[MessageReply] = {
    println(s"Goodbye message received from ${request.name}")
    Future.successful(MessageReply(s"See you soon ${request.name}!"))
  }

  // rpc LotsOfReplies(MessageRequest) returns (stream MessageReply) {}
  def lotsOfReplies(
      request: MessageRequest,
      responseObserver: StreamObserver[MessageReply]): Unit = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val tick = new Runnable {
      val counter = new AtomicInteger(10)
      def run(): Unit = {
        val n: Int = counter.getAndDecrement()

        if (n >= 0) {
          responseObserver.onNext(
            MessageReply(
              s"[$n] I'm sorry to be a bore, but I wanted to say hi again ${request.name}!"))
        } else {
          scheduler.shutdown()
          responseObserver.onCompleted()
        }
      }
    }

    scheduler.scheduleAtFixedRate(tick, initialDelay, interval, TimeUnit.MILLISECONDS)
    (): Unit
  }

  // rpc LotsOfGreetings(stream MessageRequest) returns (MessageReply) {}
  override def lotsOfGreetings(
      responseObserver: StreamObserver[MessageReply]): StreamObserver[MessageRequest] =
    new StreamObserver[MessageRequest] {
      val loggerInfo = "lotsOfGreetings"
      val counter    = new AtomicInteger(0)

      override def onError(t: Throwable): Unit =
        println(s"[$loggerInfo] Streaming failure: ${t.getMessage}")

      override def onCompleted(): Unit = {
        println(s"[$loggerInfo] Streaming completed.")

        responseObserver.onNext(MessageReply(s"$loggerInfo - It's done ;)"))
        responseObserver.onCompleted()
      }

      override def onNext(value: MessageRequest): Unit =
        println(s"[$loggerInfo] This is your message ${counter.incrementAndGet()}, ${value.name}")
    }

  // rpc BidiHello(stream MessageRequest) returns (stream MessageReply) {}
  override def bidiHello(
      responseObserver: StreamObserver[MessageReply]): StreamObserver[MessageRequest] =
    new StreamObserver[MessageRequest] {
      val loggerInfo = "bidiHello"
      val counter    = new AtomicInteger(0)

      override def onError(t: Throwable): Unit =
        println(s"[$loggerInfo] Streaming failure: ${t.getMessage}")

      override def onCompleted(): Unit = {
        println(s"[$loggerInfo] Streaming completed.")

        responseObserver.onNext(MessageReply(s"$loggerInfo - It's done ;)"))
        responseObserver.onCompleted()
      }

      override def onNext(value: MessageRequest): Unit = {
        responseObserver.onNext(MessageReply(s"$loggerInfo - I did receive $value"))
        println(s"[$loggerInfo] This is your message ${counter.incrementAndGet()}, ${value.name}")
      }
    }
}
