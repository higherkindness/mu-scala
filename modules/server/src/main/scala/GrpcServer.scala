/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
package server

import freestyle.tagless.tagless
import cats.~>
import io.grpc._

import scala.concurrent.duration.TimeUnit

trait GrpcServer[F[_]] { self =>

  def start(): F[Server]

  def getPort: F[Int]

  def getServices: F[List[ServerServiceDefinition]]

  def getImmutableServices: F[List[ServerServiceDefinition]]

  def getMutableServices: F[List[ServerServiceDefinition]]

  def shutdown(): F[Server]

  def shutdownNow(): F[Server]

  def isShutdown: F[Boolean]

  def isTerminated: F[Boolean]

  def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): F[Boolean]

  def awaitTermination(): F[Unit]

  def mapK[G[_]](fk: F ~> G): GrpcServer[G] = new GrpcServer[G] {
    def start(): G[Server] = fk(self.start)

    def getPort: G[Int] = fk(self.getPort)

    def getServices: G[List[ServerServiceDefinition]] = fk(self.getServices)

    def getImmutableServices: G[List[ServerServiceDefinition]] = fk(self.getImmutableServices)

    def getMutableServices: G[List[ServerServiceDefinition]] = fk(self.getMutableServices)

    def shutdown(): G[Server] = fk(self.shutdown)

    def shutdownNow(): G[Server] = fk(self.shutdownNow)

    def isShutdown: G[Boolean] = fk(self.isShutdown)

    def isTerminated: G[Boolean] = fk(self.isTerminated)

    def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): G[Boolean] =
      fk(self.awaitTerminationTimeout(timeout, unit))

    def awaitTermination(): G[Unit] = fk(self.awaitTermination)
  }
}

object GrpcServer {

  trait Handler[G[_]] extends GrpcServer[G]

  def apply[F[_]](implicit F: GrpcServer[F]): GrpcServer[F] = F

}
