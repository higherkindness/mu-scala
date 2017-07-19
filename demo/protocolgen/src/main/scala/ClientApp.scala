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

import cats.~>
import freestyle.rpc.demo.protocolgen.protocols._
import io.grpc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import freestyle.async.implicits._
import freestyle.rpc.client.implicits._
import monix.eval.Task
import monix.execution.Scheduler

object ClientApp {

  def main(args: Array[String]): Unit = {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val scheduler: Scheduler = monix.execution.Scheduler.Implicits.global

    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext(true).build

    val client: GreetingService.Client[Future] =
      GreetingService.client[Future](channel)

    val result = Await.result(client.sayHello(MessageRequest("hi", None)), Duration.Inf)
    println(result)

    (): Unit
  }
}
