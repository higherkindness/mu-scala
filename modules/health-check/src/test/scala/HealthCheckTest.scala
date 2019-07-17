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

import cats.effect.{IO, Resource}
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import org.scalatest.{FunSuite, Matchers, OneInstancePerTest}
import higherkindness.mu.rpc.testing.servers.withServerChannel
import higherkindness.mu.rpc.healthcheck.handler._
import higherkindness.mu.rpc.healthcheck.{HealthCheck, HealthCheckService, HealthStatus, SERVING}

class HealthCheckTest extends FunSuite with Matchers with OneInstancePerTest {
//TODO: No va: com.google.protobuf.InvalidProtocolBufferException: While parsing a protocol message, the input ended unexpectedly in the middle of a field.  This could mean either that the input has been truncated or that an embedded message misreported its own length.
  implicit val serviceBuild: HealthCheckService[IO] =
    HealthService.buildInstance[IO].unsafeRunSync()

  def withClient[Client, A](
      serviceDef: IO[ServerServiceDefinition],
      resourceBuilder: IO[ManagedChannel] => Resource[IO, Client]
  )(f: Client => A): A =
    withServerChannel(serviceDef)
      .flatMap(sc => resourceBuilder(IO(sc.channel)))
      .use(client => IO(f(client)))
      .unsafeRunSync()

  def setStatusTest(ask: HealthStatus, expected: Boolean): Boolean =
    withClient(
      HealthCheckService.bindService[IO],
      HealthCheckService.clientFromChannel[IO](_)
    ) { _.setStatus(ask).unsafeRunSync() }

  test("Get a valid response when a proper request is passed III") {
    assert(
      setStatusTest(HealthStatus(HealthCheck("testIII"), SERVING), true)
    )
  }
}
