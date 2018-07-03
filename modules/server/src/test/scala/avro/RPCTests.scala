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
import freestyle.rpc.testing.servers.withServerChannel
import io.grpc.ServerServiceDefinition
import org.scalatest._
import shapeless.{:+:, CNil, Coproduct}

class RPCTests extends RpcBaseTestSuite {

  import freestyle.rpc.avro.Utils._
  import freestyle.rpc.avro.Utils.implicits._

  def runTestProgram[A](ssd: ServerServiceDefinition, response: A)(
      f: service.RPCService.Client[ConcurrentMonad] => A): Assertion = {
    withServerChannel(ssd) { sc =>
      f(service.RPCService.clientFromChannel[ConcurrentMonad](sc.channel)) shouldBe response
    }
  }

  "An AvroWithSchema service with an updated request model" can {

    "add a new non-optional field, and" should {
      "be able to respond to an outdated request without the new value" in {
        runTestProgram(serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad], response) {
          client =>
            client.get(request).unsafeRunSync()
        }
      }
      "be able to respond to an outdated request without the new value within a coproduct" in {
        runTestProgram(
          serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "add a new optional field, and" should {
      "be able to respond to an outdated request without the new optional value" in {
        runTestProgram(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[ConcurrentMonad],
          response) { client =>
          client.get(request).unsafeRunSync()
        }
      }
      "be able to respond to an outdated request without the new optional value within a coproduct" in {
        runTestProgram(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "add a new item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runTestProgram(
          serviceRequestAddedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "remove an item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runTestProgram(
          serviceRequestRemovedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }

      "be able to respond to an outdated request with the removed valued of the previous coproduct" in {
        runTestProgram(
          serviceRequestRemovedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproductInt).unsafeRunSync()
        }
      }

    }

    "remove an existing field, and" should {
      "be able to respond to an outdated request with the old value" in {
        runTestProgram(serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad], response) {
          client =>
            client.get(request).unsafeRunSync()
        }
      }
      "be able to respond to an outdated request with the old value within a coproduct" in {
        runTestProgram(
          serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "replace the type of a field, and" should {
      "be able to respond to an outdated request with the previous value" in {
        runTestProgram(serviceRequestReplacedType.RPCService.bindService[ConcurrentMonad], response) {
          client =>
            client.get(request).unsafeRunSync()
        }
      }
      "be able to respond to an outdated request with the previous value within a coproduct" in {
        runTestProgram(
          serviceRequestReplacedType.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "rename an existing field, and" should {
      "be able to respond to an outdated request with the previous name" in {
        runTestProgram(serviceRequestRenamedField.RPCService.bindService[ConcurrentMonad], response) {
          client =>
            client.get(request).unsafeRunSync()
        }
      }
      "be able to respond to an outdated request with the previous name within a coproduct" in {
        runTestProgram(
          serviceRequestRenamedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

  }

  "An AvroWithSchema service with an updated response model" can {

    "add a new non-optional field, and" should {
      "be able to provide a compatible response" in {
        runTestProgram(
          serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad],
          response) { client =>
          client.get(request).unsafeRunSync()
        }
      }
      "be able to provide a compatible response within a coproduct" in {
        runTestProgram(
          serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "add a new item in a coproduct, and" should {
      "be able to provide a compatible response within a coproduct" in {
        runTestProgram(
          serviceResponseAddedBooleanCoproduct.RPCService.bindService[ConcurrentMonad],
          ResponseCoproduct(Coproduct[Response :+: Int :+: String :+: CNil](0))) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "remove an item in a coproduct, and" should {
      "be able to provide a compatible response" in {
        runTestProgram(
          serviceResponseRemovedIntCoproduct.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "change the type of a field, and" should {
      "be able to provide a compatible response" in {
        runTestProgram(
          serviceResponseReplacedType.RPCService.bindService[ConcurrentMonad],
          response) { client =>
          client.get(request).unsafeRunSync()
        }
      }
      "be able to provide a compatible response within a coproduct" in {
        runTestProgram(
          serviceResponseReplacedType.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "rename a field, and" should {
      "be able to provide a compatible response" in {
        runTestProgram(
          serviceResponseRenamedField.RPCService.bindService[ConcurrentMonad],
          response) { client =>
          client.get(request).unsafeRunSync()
        }
      }
      "be able to provide a compatible response within a coproduct" in {
        runTestProgram(
          serviceResponseRenamedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

    "drop a field, and" should {
      "be able to provide a compatible response" in {
        runTestProgram(
          serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad],
          response) { client =>
          client.get(request).unsafeRunSync()
        }
      }
      "be able to provide a compatible response within a coproduct" in {
        runTestProgram(
          serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response)) { client =>
          client.getCoproduct(requestCoproduct(request)).unsafeRunSync()
        }
      }
    }

  }

}
