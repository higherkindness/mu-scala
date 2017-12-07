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
package client

import freestyle.free._
import io.grpc.{CallOptions, ClientCall, MethodDescriptor}

@free
trait ChannelM {

  def newCall[I, O](
      methodDescriptor: MethodDescriptor[I, O],
      callOptions: CallOptions): FS[ClientCall[I, O]]

  def authority: FS[String]

}
