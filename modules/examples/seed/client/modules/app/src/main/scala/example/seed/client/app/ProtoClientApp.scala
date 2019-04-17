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

package example.seed.client.app

import cats.effect._
import example.seed.client.common.models._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

class ProtoClientProgram[F[_]: ConcurrentEffect: ContextShift: Timer] extends ClientBoot[F] {

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): Stream[F, ExitCode] = {
    for {
      peopleClient <- protoPeopleServiceClient(config.client.host, config.client.port)
      _            <- Stream.eval(peopleClient.getPerson(config.params.request))
      _            <- peopleClient.getRandomPersonStream
    } yield ExitCode.Success
  }
}

object ProtoClientApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    new ProtoClientProgram[IO]
      .runProgram(args)
      .compile
      .toList
      .map(_.headOption.getOrElse(ExitCode.Error))
}
