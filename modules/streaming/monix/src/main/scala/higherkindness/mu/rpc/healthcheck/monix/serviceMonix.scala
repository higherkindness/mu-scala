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

package higherkindness.mu.rpc.healthcheck.monix

import higherkindness.mu.rpc.healthcheck.unary.handler._
import higherkindness.mu.rpc.protocol.{service, Empty}
import monix.reactive.Observable

object serviceMonix {

  @service(Protobuf)
  trait HealthCheckServiceMonix[F[_]] {

    def check(service: HealthCheck): F[ServerStatus]
    def setStatus(newStatus: HealthStatus): F[Unit]
    def clearStatus(service: HealthCheck): F[Unit]

    def checkAll(empty: Empty.type): F[AllStatus]
    def cleanAll(empty: Empty.type): F[Unit]

    def watch(service: HealthCheck): F[Observable[HealthStatus]]
  }
}
