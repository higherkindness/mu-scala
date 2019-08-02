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

package higherkindness.mu.rpc.healthcheck.fs2

import cats.effect.IO
import cats.effect.concurrent.Ref
import fs2._
import fs2.concurrent.Topic
import higherkindness.mu.rpc.healthcheck.fs2.handler.HealthServiceFS2
import higherkindness.mu.rpc.healthcheck.unary.handler.{HealthCheck, HealthStatus, ServerStatus}
import org.scalatest.{Matchers, WordSpec}

class HealthCheckFS2Test extends WordSpec with Matchers {

  "FS2 health check service" should {

    val EC: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global

    implicit val cs: cats.effect.ContextShift[cats.effect.IO] =
      cats.effect.IO.contextShift(EC)

    val handler = HealthServiceFS2.buildInstance[IO]
    val hc      = new HealthCheck("example")
    val hc0     = new HealthCheck("FirstStatus")

    "work with setStatus and watch" in {
      {
        for {
          hand <- handler
          v1   <- hand.watch(hc0).take(1).compile.toList
          _    <- hand.setStatus(HealthStatus(hc, ServerStatus("NOT_SERVING")))
          v2   <- hand.watch(hc).take(1).compile.toList
        } yield (v1, v2)
      }.unsafeRunSync() shouldBe Tuple2(
        List(HealthStatus(hc0, ServerStatus("UNKNOWN"))),
        List(HealthStatus(hc, ServerStatus("NOT_SERVING")))
      )
    }
  }
}
