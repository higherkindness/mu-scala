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
package user

import freestyle.rpc.demo.user.UserServiceGrpc.UserServiceStub
import io.grpc.ManagedChannelBuilder

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class UserClient {

  // This channel construction is pending to be changed once streaming is supported
  private[this] val channel =
    ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext(true).build

  private[this] val client: UserServiceStub = UserServiceGrpc.stub(channel)

  def login(request: UserPassword): Future[User] =
    client.login(request)
}
