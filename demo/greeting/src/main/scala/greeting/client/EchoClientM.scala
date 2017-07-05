/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc.demo
package greeting.client

import freestyle._
import freestyle.rpc.client._
import freestyle.rpc.demo.echo.EchoServiceGrpc
import freestyle.rpc.demo.echo_messages._
import io.grpc.CallOptions

@module
trait EchoClientM {

  val clientCallsM: ClientCallsM
  val channelOps: ChannelM

  def echo(
      request: EchoRequest,
      options: CallOptions = CallOptions.DEFAULT): FS.Seq[EchoResponse] =
    for {
      call     <- channelOps.newCall(EchoServiceGrpc.METHOD_ECHO, options)
      response <- clientCallsM.asyncM(call, request)
    } yield response

}
