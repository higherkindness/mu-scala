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

package freestyle.rpc
package client
package config

import cats.instances.try_._
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.config.implicits._

import scala.util.{Success, Try}

class ChannelConfigTests extends RpcClientTestSuite {

  "ChannelConfig" should {

    "for Address [host, port] work as expected" in {

      ConfigForAddress[ChannelConfig.Op](SC.host, SC.port.toString)
        .interpret[Try] shouldBe a[Success[_]]
    }

    "for Target work as expected" in {

      ConfigForTarget[ChannelConfig.Op](SC.host)
        .interpret[Try] shouldBe a[Success[_]]
    }

  }

}
