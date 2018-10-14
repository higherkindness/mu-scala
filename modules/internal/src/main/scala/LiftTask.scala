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

package mu.rpc
package internal

import cats.effect.LiftIO
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext

trait LiftTask[F[_]] {
  def liftTask[A](task: Task[A]): F[A]
}

object LiftTask {
  implicit val taskLiftTask: LiftTask[Task] =
    new LiftTask[Task] {
      def liftTask[A](task: Task[A]): Task[A] = task
    }

  implicit def effectLiftTask[F[_]](implicit F: LiftIO[F], EC: ExecutionContext): LiftTask[F] =
    new LiftTask[F] {
      def liftTask[A](task: Task[A]): F[A] = F.liftIO(task.toIO(Scheduler(EC)))
    }
}
