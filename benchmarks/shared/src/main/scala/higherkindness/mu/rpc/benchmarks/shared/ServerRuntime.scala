/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.benchmarks
package shared

import cats.effect._
import cats.instances.list._
import cats.syntax.traverse._
import higherkindness.mu.rpc.benchmarks.shared.protocols._
import higherkindness.mu.rpc.benchmarks.shared.server._
import higherkindness.mu.rpc.server._

import scala.concurrent.ExecutionContext
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

trait ServerRuntime {

  val grpcPort: Int = 12345

  val EC: ExecutionContext = ExecutionContext.Implicits.global

  implicit val logger: Logger[IO]        = Slf4jLogger.getLogger[IO]
  implicit lazy val timer: Timer[IO]     = IO.timer(EC)
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(EC)

  implicit val persistenceService: PersistenceService[IO] = PersistenceService[IO]

  implicit private val pbHandler: ProtoHandler[IO]  = new ProtoHandler[IO]
  implicit private val avroHandler: AvroHandler[IO] = new AvroHandler[IO]
  implicit private val avroWithSchemaHandler: AvroWithSchemaHandler[IO] =
    new AvroWithSchemaHandler[IO]

  implicit lazy val grpcConfigsAvro: IO[List[GrpcConfig]] = List(
    PersonServicePB.bindService[IO].map(AddService),
    PersonServiceAvro.bindService[IO].map(AddService),
    PersonServiceAvroWithSchema.bindService[IO].map(AddService)
  ).sequence[IO, GrpcConfig]

  def startServer: IO[Unit] =
    for {
      _          <- logger.info("starting server..")
      serviceDef <- grpcConfigsAvro
      server     <- GrpcServer.default[IO](grpcPort, serviceDef)
      _          <- Resource.liftF(GrpcServer.server[IO](server)).use(_ => IO.never).start
      _          <- logger.info("server started..")
    } yield ()
}
