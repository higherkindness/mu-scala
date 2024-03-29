/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.config.channel

import cats.effect.IO
import higherkindness.mu.rpc.{ChannelForAddress, ChannelForTarget}
import munit.CatsEffectSuite

class ChannelConfigTests extends CatsEffectSuite {

  val port: Int    = 50051
  val host: String = "localhost"

  test("ChannelConfig for Address [host, port] work as expected") {
    ConfigForAddress[IO](
      host,
      port.toString
    ).assertEquals(ChannelForAddress("localhost", 50051))
  }

  test("ChannelConfig for Target work as expected") {
    ConfigForTarget[IO](host).assertEquals(ChannelForTarget("target"))
  }

}
