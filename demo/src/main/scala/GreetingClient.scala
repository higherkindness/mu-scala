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

import java.util.concurrent.{CountDownLatch, TimeUnit}

import io.grpc.ManagedChannelBuilder
import freestyle.rpc.demo.greeting.GreeterGrpc._
import io.grpc.stub.StreamObserver

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.Random

class GreetingClient(host: String, port: Int) {

  private[this] val channel =
    ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build

  private[this] val asyncHelloClient: GreeterStub = GreeterGrpc.stub(channel)

  def unaryDemo(request: MessageRequest): Unit = {

    val response = for {
      hi  <- asyncHelloClient.sayHello(request)
      bye <- asyncHelloClient.sayGoodbye(request)
    } yield (hi.message, bye.message)

    println("")
    println(s"Received -> ${Await.result(response, Duration.Inf)}")
    println("")
  }

  def serverStreamingDemo(request: MessageRequest): Future[Unit] = {
    val lotOfRepliesStreamingPromise = Promise[Unit]()
    val lotOfRepliesObserver = new StreamObserver[MessageReply] {

      override def onError(t: Throwable): Unit =
        println(s"[lotOfRepliesObserver] Streaming failure: ${t.getMessage}")

      override def onCompleted(): Unit = {
        println("[lotOfRepliesObserver] Lot of Replies streaming completed")
        lotOfRepliesStreamingPromise.success((): Unit)
      }

      override def onNext(value: MessageReply): Unit =
        println(s"[lotOfRepliesObserver] Received by streaming -> $value")
    }

    asyncHelloClient.lotsOfReplies(request, lotOfRepliesObserver)

    Await.ready(lotOfRepliesStreamingPromise.future, Duration.Inf)
  }

  def clientStreamingDemo(): Boolean = {
    val countDownLatch = new CountDownLatch(1)
    val responseObserver = new StreamObserver[MessageReply] {

      override def onError(t: Throwable): Unit = {
        println(s"[responseObserver] Streaming failure: ${t.getMessage}")
        countDownLatch.countDown()
      }

      override def onCompleted(): Unit = {
        println("[responseObserver] Lot of greetings streaming completed")
        countDownLatch.countDown()
      }

      override def onNext(value: MessageReply): Unit =
        println(s"[responseObserver] Received by streaming -> $value")
    }

    val requestObserver = asyncHelloClient.lotsOfGreetings(responseObserver)

    val randomRequestList = 1 to math.min(5, Random.nextInt(20))

    try {
      randomRequestList foreach (i => requestObserver.onNext(MessageRequest(s"I'm Freestyle $i")))
    } catch {
      case t: Throwable =>
        countDownLatch.countDown()
        requestObserver.onError(t)
    }

    requestObserver.onCompleted()
    countDownLatch.await(1, TimeUnit.MINUTES)
  }

  def biStreamingDemo(): Boolean = {

    val countDownLatch = new CountDownLatch(1)
    val requestObserver = asyncHelloClient.bidiHello(new StreamObserver[MessageReply] {

      override def onError(t: Throwable): Unit = {
        println(s"Bi-Streaming failure: ${t.getMessage}")
        countDownLatch.countDown()
      }

      override def onCompleted(): Unit = {
        println("Finished Bi-streaming")
        countDownLatch.countDown()
      }

      override def onNext(value: MessageReply): Unit =
        println(s"Got $value from server")
    })

    val randomRequestList: immutable.Seq[MessageRequest] = (1 to math.min(5, Random.nextInt(20))) map (
        i => MessageRequest(s"Message $i"))

    try {
      for (request <- randomRequestList) {
        println(s"Sending message $request")
        requestObserver.onNext(request)
      }
    } catch {
      case e: RuntimeException =>
        requestObserver.onError(e)
        throw e
    }

    requestObserver.onCompleted()
    countDownLatch.await(1, TimeUnit.MINUTES)
  }
}
