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
package testing

import io.grpc.internal.NoopServerCall.NoopServerCallListener
import io.grpc.{Metadata, ServerCall, ServerCallHandler, ServerServiceDefinition}

object servers {

  def serverCallHandler[Req, Res]: ServerCallHandler[Req, Res] = new ServerCallHandler[Req, Res] {
    override def startCall(
        call: ServerCall[Req, Res],
        headers: Metadata): ServerCall.Listener[Req] = new NoopServerCallListener[Req]
  }

  def serverServiceDefinition(
      serviceName: String,
      methodList: List[String]): ServerServiceDefinition = {
    val ssdBuilder = ServerServiceDefinition.builder(serviceName)
    methodList.foreach { methodName =>
      ssdBuilder.addMethod(methods.voidMethod(Some(methodName)), serverCallHandler[Void, Void])
    }
    ssdBuilder.build()
  }

}
