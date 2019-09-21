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

package higherkindness.mu.rpc.healthcheck.client

import cats.effect.{ExitCode, IO, IOApp}
import higherkindness.mu.rpc.healthcheck.client.fs2.ClientProgramFS2
import higherkindness.mu.rpc.healthcheck.client.monix.ClientProgramMonix

object ClientApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val (stream, mode, who) = args.length match {
      case 2 => (args.head, args(1), "")
      case 3 => (args.head, args(1), args(2))
      case _ => ("", "", "")
    }

    val clientProgram: IO[Unit] = stream match {
      case "fs2" =>
        import fs2.gclientFS2.implicits._
        ClientProgramFS2.clientProgramIO(who, mode)
      case "monix" =>
        import monix.gclientMonix.implicits._
        ClientProgramMonix.clientProgramIO(who, mode)
    }
    clientProgram.attempt.map(_.fold(_ => ExitCode.Error, _ => ExitCode.Success))

  }

}
