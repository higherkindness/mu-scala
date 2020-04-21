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

import cats.effect.{IO, Resource}
import cats.implicits._
import higherkindness.mu.rpc.protocol.Empty
import java.util.concurrent.TimeUnit

import higherkindness.mu.rpc.benchmarks.shared.Utils._
import higherkindness.mu.rpc.benchmarks.shared.models._
import higherkindness.mu.rpc.benchmarks.shared.protocols.PersonServicePB
import higherkindness.mu.rpc.benchmarks.shared.Runtime
import higherkindness.mu.rpc.benchmarks.shared.server._
import higherkindness.mu.rpc.testing.servers.ServerChannel
import cats.effect.{ExitCode, IO, IOApp, Resource}
import higherkindness.mu.rpc._
import cats.effect.{ExitCode, IO, IOApp}
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import cats.effect.ConcurrentEffect

object ProtoTest extends ServerImplicits {

  org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger()
  org.slf4j.bridge.SLF4JBridgeHandler.install()

  val julLogger = java.util.logging.Logger.getLogger("org.wombat")
  julLogger.fine("hello world")

  System.setProperty("io.grpc.netty.shaded.io.grpc.netty.useCustomAllocator", "false")
  //System.setProperty("io.grpc.netty.useCustomAllocator", "true")
  implicit val handler: ProtoHandler[IO] = new ProtoHandler[IO]

  def start(): IO[ExitCode] =
    for {
      _          <- logger.debug("starting server..")
      serviceDef <- grpcConfigsAvro
      server     <- GrpcServer.default[IO](12345, serviceDef)
      _          <- GrpcServer.server[IO](server)
      _          <- logger.debug("server started..")
    } yield ExitCode.Success

  val channelFor: ChannelFor = ChannelForAddress("localhost", 12345) // 1
  val clientIO: Resource[IO, PersonServicePB[IO]] =
    PersonServicePB.client[IO](channelFor)

  def useClient: IO[Unit] = clientIO.use { c =>
    for {
      _ <- logger.debug("Listing persons")
      p <- c.listPersons(Empty)
      _ <- logger.debug(s"Result = $p")
    } yield ()
  }

  def main(args: Array[String]): Unit = {
    (for {
      _ <- Resource.liftF(start()).use(_ => IO.never).start
      _ = Thread.sleep(2000)
      r <- useClient
    } yield r).unsafeRunSync()
  }

}
