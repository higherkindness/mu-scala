/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
package client

import cats.effect.IO
import cats.data.Kleisli
import higherkindness.mu.rpc.common.SC
import io.grpc.ManagedChannel

abstract class ManagedChannelInterpreterTests extends RpcClientTestSuite {

  import implicits._

  "ManagedChannelInterpreter" should {

    "build a io.grpc.ManagedChannel based on the specified configuration, for an address" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[IO](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "build a io.grpc.ManagedChannel based on the specified configuration, for a target" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[IO](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "apply should work as expected" in {

      val channelFor: ChannelFor = ChannelForTarget(SC.host)

      val channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext())

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[IO](channelFor, channelConfigList)

      val kleisli: ManagedChannelOps[IO, String] =
        Kleisli.liftF(IO(foo))

      managedChannelInterpreter[String](kleisli).unsafeRunSync shouldBe foo
    }

    "build a io.grpc.ManagedChannel based on any configuration combination" in {

      val channelFor: ChannelFor = ChannelForAddress(SC.host, SC.port)

      val channelConfigList: List[ManagedChannelConfig] = managedChannelConfigAllList

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[IO](channelFor, channelConfigList)

      val mc: ManagedChannel = managedChannelInterpreter.unsafeBuild

      mc shouldBe a[ManagedChannel]

      mc.shutdownNow()
    }

    "throw an exception when ChannelFor is not recognized" in {

      val channelFor: ChannelFor = ChannelForPort(SC.port)

      val managedChannelInterpreter =
        new ManagedChannelInterpreter[IO](channelFor, Nil)

      an[IllegalArgumentException] shouldBe thrownBy(managedChannelInterpreter.build.unsafeRunSync)
    }
  }
}
