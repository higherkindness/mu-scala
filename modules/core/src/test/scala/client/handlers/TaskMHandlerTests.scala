/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
package client.handlers

import cats.~>
import freestyle.rpc.client.RpcClientTestSuite
import freestyle.rpc.client.implicits._
import monix.eval.Task

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TaskMHandlerTests extends RpcClientTestSuite {

  import implicits._

  "TaskMHandler" should {

    "transform monix.eval.Task into any effect F, if an implicit evidence of " +
      "freestyle.async.AsyncContext[F] is provided" in {

      implicit val S: monix.execution.Scheduler = monix.execution.Scheduler.Implicits.global

      val handler: Task ~> Future = task2Future
      val fooTask: Task[String]   = Task.now(foo)

      Await.result(handler(fooTask), Duration.Inf) shouldBe foo
    }

    "recover from a failed monix.eval.Task wrapping them into scala.concurrent.Future" in {

      implicit val S: monix.execution.Scheduler = monix.execution.Scheduler.Implicits.global

      val handler: Task ~> Future  = task2Future
      val taskFailed: Task[String] = Task.raiseError(new RuntimeException(failureMessage))

      Await.result(handler(taskFailed) recover {
        case _ => failureMessage
      }, Duration.Inf) shouldBe failureMessage
    }

  }

}
