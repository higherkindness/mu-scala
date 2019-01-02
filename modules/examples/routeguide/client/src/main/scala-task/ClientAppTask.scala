/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package example.routeguide.client.task

import monix.eval.Task
import org.log4s._
import example.routeguide.client.ClientProgram._
import example.routeguide.client.task.implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

object ClientAppTask {

  def main(args: Array[String]): Unit = {

    val logger = getLogger

    logger.info(s"${Thread.currentThread().getName} Starting client, interpreting to Task ...")

    Await.result(clientProgram[Task].runToFuture, Duration.Inf)

    logger.info(s"${Thread.currentThread().getName} Finishing program interpretation ...")

    System.in.read()
    (): Unit
  }

}
