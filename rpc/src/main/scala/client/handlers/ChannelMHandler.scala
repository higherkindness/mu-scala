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

package freestyle.rpc
package client.handlers

import cats.data.Kleisli
import freestyle.free.Capture
import freestyle.rpc.client.{ChannelM, ManagedChannelOps}
import io.grpc.{CallOptions, ClientCall, MethodDescriptor}

class ChannelMHandler[M[_]](implicit C: Capture[M])
    extends ChannelM.Handler[ManagedChannelOps[M, ?]] {

  def newCall[I, O](
      methodDescriptor: MethodDescriptor[I, O],
      callOptions: CallOptions): ManagedChannelOps[M, ClientCall[I, O]] =
    Kleisli(ch => C.capture(ch.newCall(methodDescriptor, callOptions)))

  def authority: ManagedChannelOps[M, String] =
    Kleisli(ch => C.capture(ch.authority()))
}
