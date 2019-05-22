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

package example.seed.server.app

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import example.seed.config.ConfigService
import example.seed.server.common.models._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.auto._

abstract class ServerBoot[F[_]: ConcurrentEffect] {

  def runProgram(args: List[String]): F[ExitCode] =
    for {
      config   <- ConfigService[F].serviceConfig[SeedServerConfig]
      logger   <- Slf4jLogger.fromName[F](config.name)
      exitCode <- serverProgram(config)(logger)
    } yield exitCode

  def serverProgram(config: SeedServerConfig)(implicit L: Logger[F]): F[ExitCode]
}
