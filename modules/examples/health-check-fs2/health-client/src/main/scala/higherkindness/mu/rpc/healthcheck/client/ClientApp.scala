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

/*
  RUN STREAMING EXAMPLE
  Four different terminals, run (in order of appearance):
    1.- sbt health-server/run
    2.- sbt "health-client/run 1 watch"
    3.- sbt "health-client/run 2 watch"
    4.- sbt "health-client/run 1"
  Expect:
    1.- it runs service
    2.- it watches "example1" service to update health status
    3.- it watches "example2" service to update health status
    4.- it updates health status for example 1
 */

package higherkindness.mu.rpc.healthcheck.client

import cats.effect.{ExitCode, IO, IOApp}
import higherkindness.mu.rpc.healthcheck.client.ClientProgram._
import higherkindness.mu.rpc.healthcheck.client.gclient.implicits._

object ClientApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val (mode, who) = args.length match {
      case 1 => (args.head, "")
      case 2 => (args.head, args(1))
      case _ => ("", "")
    }

    clientProgramIO(who, mode).attempt
      .map(_.fold({ e =>
        println(e.getMessage)
        println(e.getStackTrace.mkString("\n"))
        ExitCode.Error
      }, { _ =>
        ExitCode.Success
      }))

  }

}
