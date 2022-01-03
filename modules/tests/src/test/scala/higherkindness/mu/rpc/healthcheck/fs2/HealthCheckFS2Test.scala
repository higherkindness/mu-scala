/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.healthcheck.fs2

import cats.effect.IO
import higherkindness.mu.rpc.healthcheck.fs2.handler.HealthServiceFS2
import higherkindness.mu.rpc.healthcheck.unary.handler.{HealthCheck, HealthStatus, ServerStatus}
import fs2.Stream
import munit.CatsEffectSuite

class HealthCheckFS2Test extends CatsEffectSuite {

  val handler = HealthServiceFS2.buildInstance[IO]
  val hc      = new HealthCheck("example")
  val status  = HealthStatus(hc, ServerStatus("NOT_SERVING"))

  test("FS2 health check service should work with setStatus and watch") {
    (for {
      hand <- handler
      stream1 = Stream.force(hand.watch(hc))
      v1      <- stream1.take(1).compile.toList.start
      _       <- hand.setStatus(status)
      outcome <- v1.join
      result  <- outcome.embed(onCancel = IO(fail("Somehow canceled")))
    } yield result).assertEquals(List(status))
  }
}
