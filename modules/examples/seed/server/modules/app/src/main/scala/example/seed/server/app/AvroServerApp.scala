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
import example.seed.server.common.models._
import example.seed.server.process.AvroPeopleServiceHandler
import example.seed.server.protocol.avro._
import higherkindness.mu.rpc.server._
import io.chrisdavenport.log4cats.Logger

class AvroServerProgram[F[_]: ConcurrentEffect] extends ServerBoot[F] {

  def serverProgram(config: SeedServerConfig)(implicit L: Logger[F]): F[ExitCode] = {

    val serverName = s"${config.name}"

    implicit val PS: PeopleService[F] = new AvroPeopleServiceHandler[F]

    for {
      peopleService <- PeopleService.bindService[F]
      server        <- GrpcServer.default[F](config.port, List(AddService(peopleService)))
      _             <- L.info(s"$serverName - Starting server at ${config.host}:${config.port}")
      exitCode      <- GrpcServer.server(server).as(ExitCode.Success)
    } yield exitCode

  }
}

object AvroServerApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new AvroServerProgram[IO].runProgram(args)
}
