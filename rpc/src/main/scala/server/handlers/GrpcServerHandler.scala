/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc
package server.handlers

import cats.data.Kleisli
import freestyle.rpc.server.{GrpcServer, GrpcServerOps}
import freestyle.Capture
import io.grpc.{Server, ServerServiceDefinition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.TimeUnit

class GrpcServerHandler[F[_]](implicit C: Capture[F])
    extends GrpcServer.Handler[GrpcServerOps[F, ?]] {

  def start: GrpcServerOps[F, Server] = {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        shutdown
        (): Unit
      }
    })

    Kleisli(s => C.capture(s.start()))
  }

  def getPort: GrpcServerOps[F, Int] = Kleisli(s => C.capture(s.getPort))

  def getServices: GrpcServerOps[F, List[ServerServiceDefinition]] =
    Kleisli(s => C.capture(s.getServices.asScala.toList))

  def getImmutableServices: GrpcServerOps[F, List[ServerServiceDefinition]] =
    Kleisli(s => C.capture(s.getImmutableServices.asScala.toList))

  def getMutableServices: GrpcServerOps[F, List[ServerServiceDefinition]] =
    Kleisli(s => C.capture(s.getMutableServices.asScala.toList))

  def shutdown: GrpcServerOps[F, Server] = Kleisli(s => C.capture(s.shutdown()))

  def shutdownNow: GrpcServerOps[F, Server] = Kleisli(s => C.capture(s.shutdownNow()))

  def isShutdown: GrpcServerOps[F, Boolean] = Kleisli(s => C.capture(s.isShutdown))

  def isTerminated: GrpcServerOps[F, Boolean] = Kleisli(s => C.capture(s.isTerminated))

  def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): GrpcServerOps[F, Boolean] =
    Kleisli(s => C.capture(s.awaitTermination(timeout, unit)))

  def awaitTermination: GrpcServerOps[F, Unit] = Kleisli(s => C.capture(s.awaitTermination()))

}
