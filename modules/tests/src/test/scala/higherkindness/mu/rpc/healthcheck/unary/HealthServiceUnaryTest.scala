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

package higherkindness.mu.rpc.healthcheck.unary

import cats.effect.IO
import higherkindness.mu.rpc.healthcheck.unary.handler._
import higherkindness.mu.rpc.protocol.Empty
import munit.CatsEffectSuite

class HealthServiceUnaryTest extends CatsEffectSuite {

  val handler = HealthService.buildInstance[IO]
  val hc      = new HealthCheck("example")
  val hc1     = new HealthCheck("example1")
  val hc2     = new HealthCheck("example2")

  test("Unary health check service should work with setStatus and check I") {
    {
      for {
        hand         <- handler
        firstStatus  <- hand.check(hc)
        _            <- hand.setStatus(HealthStatus(hc, ServerStatus("SERVING")))
        secondStatus <- hand.check(hc)
      } yield (firstStatus, secondStatus)
    }.assertEquals(((ServerStatus("UNKNOWN"), ServerStatus("SERVING"))))
  }

  test("Unary health check service should work with  setStatus and check II") {
    {
      for {
        hand         <- handler
        firstStatus  <- hand.check(hc)
        _            <- hand.setStatus(HealthStatus(hc, ServerStatus("SERVING")))
        _            <- hand.setStatus(HealthStatus(hc, ServerStatus("NOT_SERVING")))
        secondStatus <- hand.check(hc)
      } yield (firstStatus, secondStatus)
    }.assertEquals(((ServerStatus("UNKNOWN"), ServerStatus("NOT_SERVING"))))
  }

  test("Unary health check service should work with clearStatus and check I") {
    {
      for {
        hand         <- handler
        _            <- hand.setStatus(HealthStatus(hc, ServerStatus("SERVING")))
        firstStatus  <- hand.check(hc)
        _            <- hand.clearStatus(hc)
        secondStatus <- hand.check(hc)
      } yield (firstStatus, secondStatus)
    }.assertEquals(((ServerStatus("SERVING"), ServerStatus("UNKNOWN"))))
  }

  test("Unary health check service should work with clearStatus and check II") {
    {
      for {
        hand         <- handler
        firstStatus  <- hand.check(hc)
        _            <- hand.clearStatus(hc)
        _            <- hand.clearStatus(hc)
        secondStatus <- hand.check(hc)
      } yield (firstStatus, secondStatus)
    }.assertEquals(((ServerStatus("UNKNOWN"), ServerStatus("UNKNOWN"))))
  }

  test("Unary health check service should work with checkAll I") {
    {
      for {
        hand   <- handler
        _      <- hand.setStatus(HealthStatus(hc1, ServerStatus("SERVING")))
        _      <- hand.setStatus(HealthStatus(hc2, ServerStatus("SERVING")))
        status <- hand.checkAll(Empty)
      } yield status
    }.assertEquals(
      AllStatus(
        List(HealthStatus(hc1, ServerStatus("SERVING")), HealthStatus(hc2, ServerStatus("SERVING")))
      )
    )
  }

  test("Unary health check service should work with cleanAll I") {
    {
      for {
        hand      <- handler
        _         <- hand.setStatus(HealthStatus(hc1, ServerStatus("SERVING")))
        _         <- hand.setStatus(HealthStatus(hc2, ServerStatus("SERVING")))
        statusIni <- hand.checkAll(Empty)
        _         <- hand.cleanAll(Empty)
        statusEnd <- hand.checkAll(Empty)
      } yield List(statusIni, statusEnd)
    }.assertEquals(
      List(
        AllStatus(
          List(
            HealthStatus(hc1, ServerStatus("SERVING")),
            HealthStatus(hc2, ServerStatus("SERVING"))
          )
        ),
        AllStatus(
          List.empty
        )
      )
    )
  }

}
