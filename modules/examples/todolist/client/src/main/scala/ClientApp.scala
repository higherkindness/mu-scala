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

package examples.todolist.client

import cats.effect.IO
import cats.syntax.apply._
import examples.todolist.client.ClientProgram._
import examples.todolist.client.implicits._
import org.log4s._

import scala.io.StdIn

object ClientApp {

  def main(args: Array[String]): Unit = {

    val logger: Logger = getLogger

    logger.info(s"${Thread.currentThread().getName} Starting client...")

    (pongProgram[IO] *> exampleProgram[IO]).unsafeRunSync()

    logger.info(s"${Thread.currentThread().getName} Closing client...")

    StdIn.readLine()
    (): Unit
  }

}
