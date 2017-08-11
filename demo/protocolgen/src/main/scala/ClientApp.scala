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

import java.util.concurrent.TimeUnit

import freestyle.rpc.demo.protocolgen.protocols._
import io.grpc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import freestyle.async.implicits._
import freestyle.rpc.client.implicits._
import monix.execution.Scheduler
import monix.reactive.Observable

object ClientApp {

  def main(args: Array[String]): Unit = {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val scheduler: Scheduler = monix.execution.Scheduler.Implicits.global

    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext(true).build

    val client: GreetingService.Client[Future] =
      GreetingService.client[Future](channel)

    val resultM: Future[Unit] = for {
      _ <- lotOfRepliesFuture(client)
      _ <- bidiHelloTaskFuture(client)
      _ <- client
        .sayHello(MessageRequest("hi", Some(1)))
        .map(m => println(s"1 - sayHello = $m"))
      _ <- client
        .lotsOfGreetings(Observable.fromIterable(getSampleIterable("client-streaming", 3)))
        .map(m => println(s"2 - lotsOfGreetings = $m"))
    } yield (): Unit

    Await.result(
      resultM,
      Duration.Inf
    )

    channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)

    (): Unit
  }

  private[this] def lotOfRepliesFuture(client: GreetingService.Client[Future])(
      implicit S: Scheduler): Future[Unit] =
    client
      .lotsOfReplies(MessageRequest("hi", Some(2)))
      .runF("lotsOfReplies")

  private[this] def bidiHelloTaskFuture(client: GreetingService.Client[Future])(
      implicit S: Scheduler): Future[Unit] =
    client
      .bidiHello(Observable.fromIterable(getSampleIterable("bidirectional", 4)))
      .runF("bidiHello")

  private[this] def getSampleIterable(str: String, id: Int): List[MessageRequest] =
    (1 to 10).map(i => MessageRequest(s"[$str] hello$i", Some(id))).toList

  implicit class ObservableHandlerOps[A](obs: Observable[A]) {

    def runF(svc: String)(implicit S: Scheduler): Future[Unit] =
      obs.zipWithIndex
        .map {
          case (message, index) =>
            println(s"[$svc] Received Message from Server => #$index: $message")
        }
        .onErrorHandle {
          case e: StatusRuntimeException =>
            println(s"[$svc] Unexpected RPC failure: ${e.getStatus}")
            throw e
        }
        .completedL
        .runAsync(S)
  }

}
