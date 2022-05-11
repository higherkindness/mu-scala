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

package higherkindness.mu.rpc.healthcheck

import cats.syntax.all._
import cats.effect.IO
import cats.effect.testkit.TestControl
import munit.CatsEffectSuite
import _root_.fs2.Stream
import _root_.grpc.health.v1.health._
import _root_.grpc.health.v1.health.HealthCheckResponse.ServingStatus
import scala.concurrent.duration._
import io.grpc.{Status, StatusException}

class HealthServiceTest extends CatsEffectSuite {

  val NotFound = Left(Some(Status.Code.NOT_FOUND))

  def extractStatusCode[A](response: IO[Either[Throwable, A]]): IO[Either[Option[Status.Code], A]] =
    response.map {
      _.leftMap {
        case se: StatusException => Some(se.getStatus.getCode)
        case _                   => None
      }
    }

  test("Check server's general status") {
    {
      for {
        healthService <- HealthService.build[IO]
        status        <- healthService.Check(HealthCheckRequest(""))
      } yield status
    }.assertEquals(HealthCheckResponse(ServingStatus.SERVING))
  }

  test("Check an unknown service") {
    {
      for {
        healthService <- HealthService.build[IO]
        response      <- extractStatusCode(healthService.Check(HealthCheckRequest("A")).attempt)
      } yield response
    }.assertEquals(NotFound)
  }

  test("Set a service's status") {
    {
      for {
        healthService <- HealthService.build[IO]
        status1       <- extractStatusCode(healthService.Check(HealthCheckRequest("A")).attempt)
        _             <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
        status2       <- healthService.Check(HealthCheckRequest("A"))
        _             <- healthService.setStatus(ServiceStatus("A", ServingStatus.SERVING))
        status3       <- healthService.Check(HealthCheckRequest("A"))
      } yield (status1, status2, status3)
    }.assertEquals(
      (
        NotFound,
        HealthCheckResponse(ServingStatus.NOT_SERVING),
        HealthCheckResponse(ServingStatus.SERVING)
      )
    )
  }

  test("Check status of multiple services") {
    {
      for {
        healthService <- HealthService.build[IO]
        _             <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
        _             <- healthService.setStatus(ServiceStatus("B", ServingStatus.SERVING))
        status1       <- healthService.Check(HealthCheckRequest("A"))
        status2       <- healthService.Check(HealthCheckRequest("B"))
      } yield (status1, status2)
    }.assertEquals(
      (
        HealthCheckResponse(ServingStatus.NOT_SERVING),
        HealthCheckResponse(ServingStatus.SERVING)
      )
    )
  }

  test("Clear a service's status") {
    {
      for {
        healthService <- HealthService.build[IO]
        _             <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
        status1       <- healthService.Check(HealthCheckRequest("A"))
        _             <- healthService.clearStatus("A")
        status2       <- extractStatusCode(healthService.Check(HealthCheckRequest("A")).attempt)
      } yield (status1, status2)
    }.assertEquals(
      (
        HealthCheckResponse(ServingStatus.NOT_SERVING),
        NotFound
      )
    )
  }

  test("Watch a service's status") {
    val program = for {
      healthService <- HealthService.build[IO]
      stream = Stream.force(healthService.Watch(HealthCheckRequest("A")))
      fiber  <- stream.take(4).compile.toList.timeout(5.second).start
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      result <- fiber.joinWith(onCancel = IO(fail("cancelled")))
    } yield result

    TestControl
      .executeEmbed(program)
      .assertEquals(
        List(
          HealthCheckResponse(ServingStatus.SERVICE_UNKNOWN),
          HealthCheckResponse(ServingStatus.NOT_SERVING),
          HealthCheckResponse(ServingStatus.SERVING),
          HealthCheckResponse(ServingStatus.NOT_SERVING)
        )
      )
  }

  test("Watch a service's status (multiple subscribers)") {
    val program = for {
      healthService <- HealthService.build[IO]
      stream1 = Stream.force(healthService.Watch(HealthCheckRequest("A")))
      fiber1 <- stream1.take(4).compile.toList.timeout(10.second).start
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      _      <- IO.sleep(1.second)
      stream2 = Stream.force(healthService.Watch(HealthCheckRequest("A")))
      fiber2 <- stream2.take(3).compile.toList.timeout(10.second).start
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.SERVING))
      _      <- IO.sleep(1.second)
      stream3 = Stream.force(healthService.Watch(HealthCheckRequest("A")))
      fiber3  <- stream3.take(2).compile.toList.timeout(10.second).start
      _       <- IO.sleep(1.second)
      _       <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      result1 <- fiber1.joinWith(onCancel = IO(fail("cancelled")))
      result2 <- fiber2.joinWith(onCancel = IO(fail("cancelled")))
      result3 <- fiber3.joinWith(onCancel = IO(fail("cancelled")))
    } yield (result1, result2, result3)

    TestControl
      .executeEmbed(program)
      .assertEquals(
        (
          List(
            HealthCheckResponse(ServingStatus.SERVICE_UNKNOWN),
            HealthCheckResponse(ServingStatus.NOT_SERVING),
            HealthCheckResponse(ServingStatus.SERVING),
            HealthCheckResponse(ServingStatus.NOT_SERVING)
          ),
          List(
            HealthCheckResponse(ServingStatus.NOT_SERVING),
            HealthCheckResponse(ServingStatus.SERVING),
            HealthCheckResponse(ServingStatus.NOT_SERVING)
          ),
          List(
            HealthCheckResponse(ServingStatus.SERVING),
            HealthCheckResponse(ServingStatus.NOT_SERVING)
          )
        )
      )
  }

  test("Watch a service's status (multiple services have their statuses updated)") {
    val program = for {
      healthService <- HealthService.build[IO]
      stream = Stream.force(healthService.Watch(HealthCheckRequest("A")))
      fiber  <- stream.take(4).compile.toList.timeout(10.second).start
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("B", ServingStatus.NOT_SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("B", ServingStatus.SERVING))
      _      <- IO.sleep(1.second)
      _      <- healthService.setStatus(ServiceStatus("A", ServingStatus.NOT_SERVING))
      result <- fiber.joinWith(onCancel = IO(fail("cancelled")))
    } yield result

    TestControl
      .executeEmbed(program)
      .assertEquals(
        List(
          HealthCheckResponse(ServingStatus.SERVICE_UNKNOWN),
          HealthCheckResponse(ServingStatus.NOT_SERVING),
          HealthCheckResponse(ServingStatus.SERVING),
          HealthCheckResponse(ServingStatus.NOT_SERVING)
        )
      )
  }

}
