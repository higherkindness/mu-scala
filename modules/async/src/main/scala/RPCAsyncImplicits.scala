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
package async

import cats.~>
import cats.arrow.FunctionK
import cats.effect.IO
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future

trait RPCAsyncImplicits {

  implicit val future2Task: Future ~> Task =
    λ[Future ~> Task] { fa =>
      Task.deferFuture(fa)
    }

  implicit def task2Future(implicit S: Scheduler): Task ~> Future = λ[Task ~> Future](_.runAsync)

  implicit val task2Task: Task ~> Task = FunctionK.id[Task]

  implicit def task2IO(implicit S: Scheduler): Task ~> IO = λ[Task ~> IO](_.toIO)

  implicit val io2Task: IO ~> Task = λ[IO ~> Task](_.to[Task])
}
