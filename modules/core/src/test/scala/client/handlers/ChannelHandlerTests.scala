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
package client.handlers

import freestyle.rpc.client.RpcClientTestSuite
import io.grpc.{CallOptions, MethodDescriptor}

import scala.concurrent.{ExecutionContext, Future}

class ChannelHandlerTests extends RpcClientTestSuite {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  import implicits._

  val handler: ChannelMHandler[Future] = new ChannelMHandler[Future]

  "ChannelMHandler.Handler" should {

    "allow to perform new calls" in {

      (managedChannelMock
        .newCall(_: MethodDescriptor[String, String], _: CallOptions))
        .expects(methodDescriptor, CallOptions.DEFAULT)
        .returns(clientCallMock)

      runKFuture(handler.newCall(methodDescriptor, CallOptions.DEFAULT), managedChannelMock) shouldBe clientCallMock
    }

    "allow to fetch the authority of the destination the channel connects to" in {

      (managedChannelMock.authority _: () => String).expects().returns(authority)

      runKFuture(handler.authority, managedChannelMock) shouldBe authority
    }

  }

}
