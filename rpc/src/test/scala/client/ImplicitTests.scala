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
package client

import freestyle.FSHandler
import monix.eval.Task

import scala.concurrent.Future

class ImplicitTests extends RpcClientTestSuite {

  type FSHandlerTask2Future = FSHandler[Task, Future]

  "AsyncInstances.task2Future" should {

    "provide an implicit evidence allowing to transform from monix.eval.Task to scala.concurrent.Future" in {

      implicit val S: monix.execution.Scheduler = monix.execution.Scheduler.Implicits.global

      freestyle.rpc.client.implicits.task2Future shouldBe a[FSHandlerTask2Future]
    }

    "fail compiling when the monix.execution.Scheduler implicit evidence is not present" in {

      shapeless.test.illTyped(
        """freestyle.rpc.client.implicits.task2Future""",
        ".*Cannot find an implicit Scheduler, either import monix.execution.Scheduler.Implicits.global or use a custom one.*"
      )
    }

  }

}
