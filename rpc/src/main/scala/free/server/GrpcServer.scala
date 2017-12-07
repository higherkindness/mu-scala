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

package freestyle.free.rpc
package server

import freestyle.free._
import io.grpc._

import scala.concurrent.duration.TimeUnit

@free
trait GrpcServer {

  def start(): FS[Server]

  def getPort: FS[Int]

  def getServices: FS[List[ServerServiceDefinition]]

  def getImmutableServices: FS[List[ServerServiceDefinition]]

  def getMutableServices: FS[List[ServerServiceDefinition]]

  def shutdown(): FS[Server]

  def shutdownNow(): FS[Server]

  def isShutdown: FS[Boolean]

  def isTerminated: FS[Boolean]

  def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): FS[Boolean]

  def awaitTermination(): FS[Unit]

}
