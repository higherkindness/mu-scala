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

package higherkindness.mu.rpc
package server

import cats.~>
import cats.effect.{Effect, IO, Sync}
import cats.instances.either._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}
import io.grpc.netty.NettyServerBuilder
import scala.concurrent.duration.TimeUnit

trait GrpcServer[F[_]] { self =>

  def start(): F[Unit]

  def getPort: F[Int]

  def getServices: F[List[ServerServiceDefinition]]

  def getImmutableServices: F[List[ServerServiceDefinition]]

  def getMutableServices: F[List[ServerServiceDefinition]]

  def shutdown(): F[Unit]

  def shutdownNow(): F[Unit]

  def isShutdown: F[Boolean]

  def isTerminated: F[Boolean]

  def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): F[Boolean]

  def awaitTermination(): F[Unit]

  def mapK[G[_]](fk: F ~> G): GrpcServer[G] = new GrpcServer[G] {
    def start(): G[Unit] = fk(self.start)

    def getPort: G[Int] = fk(self.getPort)

    def getServices: G[List[ServerServiceDefinition]] = fk(self.getServices)

    def getImmutableServices: G[List[ServerServiceDefinition]] = fk(self.getImmutableServices)

    def getMutableServices: G[List[ServerServiceDefinition]] = fk(self.getMutableServices)

    def shutdown(): G[Unit] = fk(self.shutdown)

    def shutdownNow(): G[Unit] = fk(self.shutdownNow)

    def isShutdown: G[Boolean] = fk(self.isShutdown)

    def isTerminated: G[Boolean] = fk(self.isTerminated)

    def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): G[Boolean] =
      fk(self.awaitTerminationTimeout(timeout, unit))

    def awaitTermination(): G[Unit] = fk(self.awaitTermination)
  }
}

object GrpcServer {

  def server[F[_]](S: GrpcServer[F])(implicit F: Effect[F]): F[Unit] = {

    def shutdownEventually(endProcess: Either[Throwable, Unit] => Unit): Unit = {
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit =
          F.runAsync(S.shutdown *> S.awaitTermination)(cb => IO(endProcess(cb.void))).unsafeRunSync
      })
    }

    S.start() *> F.async[Unit](cb => shutdownEventually(cb))
  }

  def default[F[_]](port: Int, configList: List[GrpcConfig])(
      implicit F: Sync[F]): F[GrpcServer[F]] =
    F.delay(buildServer(ServerBuilder.forPort(port), configList)).map(fromServer[F])

  def netty[F[_]](port: Int, configList: List[GrpcConfig])(implicit F: Sync[F]): F[GrpcServer[F]] =
    netty(ChannelForPort(port), configList)

  def netty[F[_]](channelFor: ChannelFor, configList: List[GrpcConfig])(
      implicit F: Sync[F]): F[GrpcServer[F]] =
    for {
      builder <- F.delay(nettyBuilder(channelFor))
      server  <- F.delay(buildNettyServer(builder, configList))
    } yield fromServer[F](server)

  def fromServer[F[_]: Sync](server: Server): GrpcServer[F] =
    handlers.GrpcServerHandler[F].mapK(λ[GrpcServerOps[F, ?] ~> F](_.run(server)))

  private[this] def buildServer(bldr: ServerBuilder[SB] forSome { type SB <: ServerBuilder[SB] }, configList: List[GrpcConfig]): Server = {
    configList
      .foldLeft(bldr) { (bldr, cfg) =>
        SBuilder(bldr)(cfg)
      }
      .build()
  }

  private[this] def buildNettyServer(
      bldr: NettyServerBuilder,
      configList: List[GrpcConfig]): Server = {
    configList
      .foldLeft(bldr) { (bldr, cfg) =>
        (SBuilder(bldr) orElse NettySBuilder(bldr))(cfg)
      }
      .build()
  }

  private[this] def nettyBuilder(initConfig: ChannelFor): NettyServerBuilder =
    initConfig match {
      case ChannelForPort(port)        => NettyServerBuilder.forPort(port)
      case ChannelForSocketAddress(sa) => NettyServerBuilder.forAddress(sa)
      case e =>
        throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
    }

}
