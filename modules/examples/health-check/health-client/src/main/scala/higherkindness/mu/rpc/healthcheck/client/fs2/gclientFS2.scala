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

package higherkindness.mu.rpc.healthcheck.client.fs2

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.ChannelFor
import higherkindness.mu.rpc.config.channel.ConfigForAddress
import higherkindness.mu.rpc.healthcheck.fs2.serviceFS2.HealthCheckServiceFS2
import higherkindness.mu.rpc.healthcheck.sFs2.CommonRuntimeFS2
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object gclientFS2 {

  trait Implicits extends CommonRuntimeFS2 {

    val channelFor: ChannelFor =
      ConfigForAddress[IO]("localhost", "50051").unsafeRunSync()

    implicit def logger: SelfAwareStructuredLogger[IO] =
      Slf4jLogger.getLogger

    val healthCheckServiceClientFS2: Resource[IO, HealthCheckServiceFS2[IO]] =
      HealthCheckServiceFS2.client[IO](channelFor)

    implicit val healthCheckClientHandlerFS2: HealthCheckClientHandlerFS2[IO] =
      new HealthCheckClientHandlerFS2[IO](healthCheckServiceClientFS2)

  }

  object implicits extends Implicits
}
