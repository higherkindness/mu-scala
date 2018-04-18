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
import io.grpc._

import scala.concurrent.duration.TimeUnit

@tagless(true)
trait GrpcServer[F[_]] {

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

}
