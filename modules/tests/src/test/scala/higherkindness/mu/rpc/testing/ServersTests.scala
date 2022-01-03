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

package higherkindness.mu.rpc
package testing

import io.grpc.{ServerCall, ServerCallHandler, ServerServiceDefinition}
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

import scala.jdk.CollectionConverters._

class ServersTests extends ScalaCheckSuite {

  import TestingUtils._

  test("servers.serverCallHandler should create a noop call handler") {
    type StringCallListener = ServerCall.Listener[String]
    val handler: ServerCallHandler[String, Int] = servers.serverCallHandler[String, Int]
    assert(Option(handler.startCall(null, null)).exists(_.isInstanceOf[StringCallListener]))
  }

  test(
    "servers.serverServiceDefinition should " +
      "create a server service definition with the provided name and methods"
  ) {

    forAllNoShrink(serverDefGen) { case (serverName, methodNameList) =>
      val serverDef: ServerServiceDefinition =
        servers.serverServiceDefinition(serverName, methodNameList)
      (serverDef.getServiceDescriptor.getName == serverName) && (serverDef.getMethods.asScala
        .map(_.getMethodDescriptor.getFullMethodName)
        .toList
        .sorted == methodNameList.sorted)
    }
  }

}
