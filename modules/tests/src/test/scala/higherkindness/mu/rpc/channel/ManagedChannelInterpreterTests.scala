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

package higherkindness.mu.rpc
package channel

import cats.effect.IO
import cats.data.Kleisli
import cats.effect.std.Dispatcher
import higherkindness.mu.rpc.common.SC
import io.grpc.ManagedChannel
import munit.CatsEffectSuite

abstract class ManagedChannelInterpreterTests extends CatsEffectSuite {

  import TestData._

  def mkInterpreter(
      channelFor: ChannelFor,
      channelConfigList: List[ManagedChannelConfig]
  ): ManagedChannelInterpreter[IO]

  val dispatcher = ResourceFixture(Dispatcher[IO])

  dispatcher.test(
    "ManagedChannelInterpreter build a io.grpc.ManagedChannel based on the specified configuration, for an address"
  ) { disp =>
    val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

    val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

    val managedChannelInterpreter = mkInterpreter(channelFor, channelConfigList)

    val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild(disp)

    IO(mc.shutdownNow()).map(_ => assert(Option(mc).nonEmpty))
  }

  dispatcher.test(
    "ManagedChannelInterpreter build a io.grpc.ManagedChannel based on the specified configuration, for a target"
  ) { disp =>
    val channelFor: ChannelFor = ChannelForTarget(SC.host)

    val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

    val managedChannelInterpreter = mkInterpreter(channelFor, channelConfigList)

    val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild(disp)

    IO(mc.shutdownNow()).map(_ => assert(Option(mc).nonEmpty))

  }

  test("ManagedChannelInterpreter apply should work as expected") {

    val channelFor: ChannelFor = ChannelForTarget(SC.host)

    val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

    val managedChannelInterpreter = mkInterpreter(channelFor, channelConfigList)

    val kleisli: ManagedChannelOps[IO, String] =
      Kleisli.liftF(IO(foo))

    managedChannelInterpreter[String](kleisli).assertEquals(foo)
  }

  dispatcher.test(
    "ManagedChannelInterpreter build a io.grpc.ManagedChannel based on any configuration combination"
  ) { disp =>
    val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

    val channelConfigList: List[ManagedChannelConfig] = TestData.managedChannelConfigAllList

    val managedChannelInterpreter = mkInterpreter(channelFor, channelConfigList)

    val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild(disp)

    IO(mc.shutdownNow()).map(_ => assert(Option(mc).nonEmpty))

  }

  test("ManagedChannelInterpreter throw an exception when ChannelFor is not recognized") {

    val channelFor: ChannelFor = ChannelForPort(SC.port)

    val managedChannelInterpreter = mkInterpreter(channelFor, Nil)

    interceptIO[IllegalArgumentException](managedChannelInterpreter.build)
  }
}
