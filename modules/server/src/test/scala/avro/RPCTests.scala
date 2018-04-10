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

import cats.Apply
import freestyle.rpc.common._
import freestyle.rpc.testing.ServerChannel.withServerChannel
import io.grpc.ServerServiceDefinition
import org.scalatest._

class RPCTests extends RpcBaseTestSuite {

  import freestyle.rpc.avro.Utils._
  import freestyle.rpc.avro.Utils.implicits._

  "frees-rpc client using avro serialization with schemas" should {

    def runTestProgram[T](ssd: ServerServiceDefinition): Assertion = {

      withServerChannel(ssd) { sc =>
        val rpcServiceClient: service.RPCService.Client[ConcurrentMonad] =
          service.RPCService.clientFromChannel[ConcurrentMonad](sc.channel)

        val (r1, r2) = Apply[ConcurrentMonad]
          .product(
            rpcServiceClient.get(request),
            rpcServiceClient.getCoproduct(requestCoproduct(request)))
          .unsafeRunSync()

        r1 shouldBe response
        r2 shouldBe responseCoproduct(response)
      }

    }

    "be able to respond to a request" in {
      runTestProgram(service.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the request model has added a boolean field" in {
      runTestProgram(serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the request model has added a string field" in {
      runTestProgram(serviceRequestAddedString.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the request model has added an int field" in {
      runTestProgram(serviceRequestAddedInt.RPCService.bindService[ConcurrentMonad])

    }

    "be able to respond to a request when the request model has added a field that is a case class" in {
      runTestProgram(serviceRequestAddedNestedRequest.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the request model has dropped a field" in {
      runTestProgram(serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the response model has added a boolean field" in {
      runTestProgram(serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the response model has added a string field" in {
      runTestProgram(serviceResponseAddedString.RPCService.bindService[ConcurrentMonad])

    }

    "be able to respond to a request when the response model has added an int field" in {
      runTestProgram(serviceResponseAddedInt.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the response model has added a field that is a case class" in {
      runTestProgram(serviceResponseAddedNestedResponse.RPCService.bindService[ConcurrentMonad])
    }

    "be able to respond to a request when the response model has dropped a field" in {
      runTestProgram(serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad])
    }
  }

}
