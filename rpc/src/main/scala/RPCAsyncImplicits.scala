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

package freestyle
package rpc

import cats.{~>, Comonad}
import freestyle.async.AsyncContext
import freestyle.rpc.client.handlers._
import journal.Logger
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait RPCAsyncImplicits extends freestyle.async.Implicits {

  protected[this] val asyncLogger: Logger            = Logger[this.type]
  protected[this] val atMostDuration: FiniteDuration = 10.seconds

  implicit def futureComonad(implicit ec: ExecutionContext): Comonad[Future] =
    new Comonad[Future] {
      def extract[A](x: Future[A]): A = {
        asyncLogger.info(s"${Thread.currentThread().getName} Waiting $atMostDuration for $x...")
        Await.result(x, atMostDuration)
      }

      override def coflatMap[A, B](fa: Future[A])(f: (Future[A]) => B): Future[B] = Future(f(fa))

      override def map[A, B](fa: Future[A])(f: (A) => B): Future[B] =
        fa.map(f)
    }

  implicit def task2Future(implicit S: Scheduler): FSHandler[Task, Future] =
    new TaskMHandler[Future]

  implicit val future2Task: Future ~> Task =
    new (Future ~> Task) {
      override def apply[A](fa: Future[A]): Task[A] = {
        asyncLogger.info(s"${Thread.currentThread().getName} Deferring Future to Task...")
        Task.deferFuture(fa)
      }
    }

  implicit val task2Task: Task ~> Task = new (Task ~> Task) {
    override def apply[A](fa: Task[A]): Task[A] = fa
  }

}
