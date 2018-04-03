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
package avro

import freestyle.rpc.common._
import freestyle.rpc.server.ServerW
import freestyle.rpc.server.implicits._
import org.scalatest._

class RPCTests extends RpcBaseTestSuite with BeforeAndAfter {

  import freestyle.rpc.avro.Utils._
  import freestyle.rpc.avro.Utils.implicits._

  "frees-rpc client using avro serialization with schemas" should {

    def runTestProgram[T](implicit serverW: ServerW) = {
      serverStart[ConcurrentMonad].unsafeRunSync
      try {
        freesRPCServiceClient.get(request).unsafeRunSync() shouldBe response
        freesRPCServiceClient
          .getCoproduct(requestCoproduct(request))
          .unsafeRunSync() shouldBe responseCoproduct(response)
      } finally {
        serverStop[ConcurrentMonad].unsafeRunSync
      }
    }

    "be able to respond to a request" in {
      implicit val serverW = createServerConf(rpcServiceConfigs)
      runTestProgram
    }

    "be able to respond to a request when the request model has added a boolean field" in {
      implicit val serverW = createServerConf(rpcServiceRequestAddedBooleanConfigs)
      runTestProgram
    }

    "be able to respond to a request when the request model has added a string field" in {
      implicit val serverW = createServerConf(rpcServiceRequestAddedStringConfigs)
      runTestProgram
    }

    "be able to respond to a request when the request model has added an int field" in {
      implicit val serverW = createServerConf(rpcServiceRequestAddedIntConfigs)
      runTestProgram
    }

    "be able to respond to a request when the request model has added a field that is a case class" in {
      implicit val serverW = createServerConf(rpcServiceRequestAddedNestedRequestConfigs)
      runTestProgram
    }

    "be able to respond to a request when the request model has dropped a field" in {
      implicit val serverW = createServerConf(rpcServiceRequestDroppedFieldConfigs)
      runTestProgram
    }

    "be able to respond to a request when the response model has added a boolean field" in {
      implicit val serverW = createServerConf(rpcServiceResponseAddedBooleanConfigs)
      runTestProgram
    }

    "be able to respond to a request when the response model has added a string field" in {
      implicit val serverW = createServerConf(rpcServiceResponseAddedStringConfigs)
      runTestProgram
    }

    "be able to respond to a request when the response model has added an int field" in {
      implicit val serverW = createServerConf(rpcServiceResponseAddedIntConfigs)
      runTestProgram
    }

    "be able to respond to a request when the response model has added a field that is a case class" in {
      implicit val serverW = createServerConf(rpcServiceResponseAddedNestedResponseConfigs)
      runTestProgram
    }

    "be able to respond to a request when the response model has dropped a field" in {
      implicit val serverW = createServerConf(rpcServiceResponseDroppedFieldConfigs)
      runTestProgram
    }
  }

}
