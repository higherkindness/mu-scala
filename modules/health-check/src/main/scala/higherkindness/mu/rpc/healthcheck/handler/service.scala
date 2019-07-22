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

package higherkindness.mu.rpc.healthcheck.handler

import higherkindness.mu.rpc.healthcheck.ServerStatus
import higherkindness.mu.rpc.protocol.{service, Protobuf}
import fs2._

object service {

  case class HealthCheck(nameService: String)
  case class HealthStatus(hc: HealthCheck, status: ServerStatus)
  case class WentNice(ok: Boolean)

  case class AllStatuses(all: List[(HealthCheck, ServerStatus)])
  case class EmptyInput() //TODO doesnt work with empty type nor case object
  @service(Protobuf)
  trait HealthCheckService[F[_]] {

    def check(service: HealthCheck): F[ServerStatus]
    def setStatus(newStatus: HealthStatus): F[WentNice]
    def clearStatus(service: HealthCheck): F[WentNice]

    def checkAll(empty: EmptyInput): F[AllStatuses]
    def cleanAll(empty: EmptyInput): F[WentNice]

    def watch(service: HealthCheck): Stream[F, HealthStatus]
  }
}
