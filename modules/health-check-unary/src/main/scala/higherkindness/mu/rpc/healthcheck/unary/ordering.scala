/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.healthcheck

import cats.kernel.Order
import cats.instances.string._
import higherkindness.mu.rpc.healthcheck.unary.handler.HealthCheck

object ordering {
  implicit val orderForHealthCheck: Order[HealthCheck] = new HealthCheckOrder

  class HealthCheckOrder(implicit O: Order[String]) extends Order[HealthCheck] {

    override def eqv(x: HealthCheck, y: HealthCheck): Boolean =
      O.eqv(x.nameService, y.nameService)

    def compare(x: HealthCheck, y: HealthCheck): Int =
      O.compare(x.nameService, y.nameService)
  }
}
