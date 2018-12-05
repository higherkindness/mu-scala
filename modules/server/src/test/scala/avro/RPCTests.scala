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

package mu.rpc
package avro

import cats.effect.IO
import io.grpc.ServerServiceDefinition
import mu.rpc.common._
import mu.rpc.testing.servers.withServerChannel
import org.scalatest._
import shapeless.{:+:, CNil, Coproduct}

class RPCTests extends RpcBaseTestSuite {

  import TestsImplicits._
  import mu.rpc.avro.Utils._
  import mu.rpc.avro.Utils.implicits._

  def runSucceedAssertion[A](ssd: ServerServiceDefinition, response: A)(
      f: service.RPCService.Client[ConcurrentMonad] => ConcurrentMonad[A]): Assertion = {
    withServerChannel(ssd) { sc =>
      service.RPCService
        .clientFromChannel[ConcurrentMonad](IO(sc.channel))
        .use(f)
        .unsafeRunSync() shouldBe response
    }
  }

  def runFailedAssertion[A](ssd: ServerServiceDefinition)(
      f: service.RPCService.Client[ConcurrentMonad] => ConcurrentMonad[A]): Assertion = {
    withServerChannel(ssd) { sc =>
      assertThrows[io.grpc.StatusRuntimeException] {
        service.RPCService.clientFromChannel[ConcurrentMonad](IO(sc.channel)).use(f).unsafeRunSync()
      }
    }
  }

  "An AvroWithSchema service with an updated request model" can {

    "add a new non-optional field, and" should {
      "be able to respond to an outdated request without the new value" in {
        runSucceedAssertion(
          serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to respond to an outdated request without the new value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new optional field, and" should {
      "be able to respond to an outdated request without the new optional value" in {
        runSucceedAssertion(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to respond to an outdated request without the new optional value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedOptionalBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestAddedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "remove an item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestRemovedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }

      "be able to respond to an outdated request with the removed valued of the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestRemovedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproductInt))
      }

    }

    "replace an item in coproduct, and" should {
      "be able to respond to an outdated request with the previous coproduct" in {
        runSucceedAssertion(
          serviceRequestReplacedCoproductItem.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }

      "be able to respond to an outdated request with the previous coproduct AAAAA" in {
        runFailedAssertion(
          serviceRequestReplacedCoproductItem.RPCService.bindService[ConcurrentMonad])(
          _.getCoproduct(requestCoproductString))
      }

    }

    "remove an existing field, and" should {
      "be able to respond to an outdated request with the old value" in {
        runSucceedAssertion(
          serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to respond to an outdated request with the old value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestDroppedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "replace the type of a field, and" should {
      "be able to respond to an outdated request with the previous value" in {
        runSucceedAssertion(
          serviceRequestReplacedType.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to respond to an outdated request with the previous value within a coproduct" in {
        runSucceedAssertion(
          serviceRequestReplacedType.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "rename an existing field, and" should {
      "be able to respond to an outdated request with the previous name" in {
        runSucceedAssertion(
          serviceRequestRenamedField.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to respond to an outdated request with the previous name within a coproduct" in {
        runSucceedAssertion(
          serviceRequestRenamedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

  }

  "An AvroWithSchema service with an updated response model" can {

    "add a new non-optional field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseAddedBoolean.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "add a new item in a coproduct, and" should {
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseAddedBooleanCoproduct.RPCService.bindService[ConcurrentMonad],
          ResponseCoproduct(Coproduct[Response :+: Int :+: String :+: CNil](0)))(
          _.getCoproduct(requestCoproduct(request)))
      }
    }

    "remove an item in a coproduct, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseRemovedIntCoproduct.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "replace an item in a coproduct, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseReplacedCoproduct.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }

    }

    "change the type of a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseReplacedType.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseReplacedType.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "rename a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseRenamedField.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseRenamedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

    "drop a field, and" should {
      "be able to provide a compatible response" in {
        runSucceedAssertion(
          serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad],
          response)(_.get(request))
      }
      "be able to provide a compatible response within a coproduct" in {
        runSucceedAssertion(
          serviceResponseDroppedField.RPCService.bindService[ConcurrentMonad],
          responseCoproduct(response))(_.getCoproduct(requestCoproduct(request)))
      }
    }

  }

}
