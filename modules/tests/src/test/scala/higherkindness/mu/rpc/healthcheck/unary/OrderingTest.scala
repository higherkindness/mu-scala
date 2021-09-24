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

package higherkindness.mu.rpc.healthcheck.unary

import cats.syntax.all._
import higherkindness.mu.rpc.healthcheck.ordering._
import higherkindness.mu.rpc.healthcheck.unary.handler.HealthCheck
import munit.FunSuite

class OrderingTest extends FunSuite {
  test("Ordering should work") {
    assert(new HealthCheck("example") === new HealthCheck("example"))
    assert(new HealthCheck("example") =!= new HealthCheck("not example"))
  }
  test("Ordering should work with boolean comparison") {
    assert(orderForHealthCheck.eqv(new HealthCheck("example"), new HealthCheck("example")))
    assert(!orderForHealthCheck.eqv(new HealthCheck("example"), new HealthCheck("no example")))
  }
  test("Ordering should work with integer comparison") {
    assert(
      orderForHealthCheck.compare(new HealthCheck("example"), new HealthCheck("example")) === 0
    )
    assert(
      orderForHealthCheck.compare(new HealthCheck("example"), new HealthCheck("no example")) < 0
    )
  }
}
