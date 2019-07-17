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

package higherkindness.mu.rpc.healthcheck.client

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.config.channel.ConfigForAddress
import higherkindness.mu.rpc.healthcheck.handler.service.HealthCheckService
import higherkindness.mu.rpc.healthcheck.server.CommonRuntime
import higherkindness.mu.rpc.ChannelFor

object gclient {

  trait Implicits extends CommonRuntime {

    val channelFor: ChannelFor =
      ConfigForAddress[IO]("rpc.client.host", "rpc.client.port").unsafeRunSync()

    val healthCheckServiceClient: Resource[IO, HealthCheckService[IO]] =
      HealthCheckService.client[IO](channelFor)

    implicit val healthCheckClientHandler: HealthCheckClientHandler[IO] =
      new HealthCheckClientHandler[IO](healthCheckServiceClient)
  }

  object implicits extends Implicits
}
