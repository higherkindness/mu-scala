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
package testing

import io.grpc.{ServerCall, ServerCallHandler, ServerServiceDefinition}
import org.scalatest._
import org.scalatest.prop.Checkers
import org.scalacheck.Prop._

import scala.collection.JavaConverters._

class ServersTests extends WordSpec with Matchers with Checkers {

  import TestingUtils._

  "servers.serverCallHandler" should {

    "create a noop call handler" in {
      type StringCallListener = ServerCall.Listener[String]
      val handler: ServerCallHandler[String, Int] = servers.serverCallHandler[String, Int]
      handler.startCall(null, null) shouldBe a[StringCallListener]
    }

  }

  "servers.serverServiceDefinition" should {

    "create a server service definition with the provided name and methods" in {

      check {
        forAllNoShrink(serverDefGen) {
          case (serverName, methodNameList) =>
            val serverDef: ServerServiceDefinition =
              servers.serverServiceDefinition(serverName, methodNameList)
            (serverDef.getServiceDescriptor.getName == serverName) && (serverDef.getMethods.asScala
              .map(_.getMethodDescriptor.getFullMethodName)
              .toList
              .sorted == methodNameList.sorted)
        }
      }
    }

  }

}
