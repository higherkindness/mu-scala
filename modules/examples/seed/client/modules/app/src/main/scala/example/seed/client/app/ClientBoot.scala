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
import cats.syntax.functor._
import example.seed.client.common.models._
import example.seed.client.process._
import example.seed.config.ConfigService
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.auto._

abstract class ClientBoot[F[_]: ConcurrentEffect: ContextShift: Timer] {

  def avroPeopleServiceClient(host: String, port: Int)(
      implicit L: Logger[F]): Stream[F, AvroPeopleServiceClient[F]] =
    AvroPeopleServiceClient.createClient(host, port)

  def protoPeopleServiceClient(host: String, port: Int)(
      implicit L: Logger[F]): Stream[F, ProtoPeopleServiceClient[F]] =
    ProtoPeopleServiceClient.createClient(host, port)

  def runProgram(args: List[String]): Stream[F, ExitCode] = {
    def setupConfig: F[SeedClientConfig] =
      ConfigService[F]
        .serviceConfig[ClientConfig]
        .map(client => SeedClientConfig(client, ClientParams.loadParams(client.name, args)))

    for {
      config   <- Stream.eval(setupConfig)
      logger   <- Stream.eval(Slf4jLogger.fromName[F](config.client.name))
      exitCode <- clientProgram(config)(logger)
    } yield exitCode
  }

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): Stream[F, ExitCode]
}
