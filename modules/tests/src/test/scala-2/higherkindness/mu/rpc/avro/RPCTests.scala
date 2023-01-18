/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc
package avro

import cats.effect.{IO, Resource}
import io.grpc.ServerServiceDefinition
import higherkindness.mu.rpc.common.{A => _}
import higherkindness.mu.rpc.testing.servers.{withServerChannel, ServerChannel}
import munit.CatsEffectSuite
import shapeless.{:+:, CNil, Coproduct}

class RPCTests extends CatsEffectSuite {

  import higherkindness.mu.rpc.avro.Utils._
  import higherkindness.mu.rpc.avro.Utils.implicits._

  def createClient(
      sc: ServerChannel
  ): Resource[IO, service.RPCService[IO]] =
    service.RPCService.clientFromChannel[IO](IO(sc.channel))

  def runSucceedAssertion[A](ssd: Resource[IO, ServerServiceDefinition], response: A)(
      f: service.RPCService[IO] => IO[A]
  ): IO[Unit] =
    withServerChannel[IO](ssd)
      .flatMap(createClient)
      .use(f)
      .assertEquals(response)

  def runFailedAssertion[A](
      ssd: Resource[IO, ServerServiceDefinition]
  )(f: service.RPCService[IO] => IO[A]): IO[io.grpc.StatusRuntimeException] =
    interceptIO[io.grpc.StatusRuntimeException] {
      withServerChannel[IO](ssd)
        .flatMap(createClient)
        .use(f)
        .void
    }

  val testParent1 = "An AvroWithSchema service with an updated request model"

  test(
    testParent1 + " add a new non-optional field, and " +
      "be able to respond to an outdated request without the new value"
  ) {
    runSucceedAssertion(serviceRequestAddedBoolean.RPCService.bindService[IO], response)(
      _.get(request)
    )
  }
  test(
    testParent1 + " add a new non-optional field, and " +
      "be able to respond to an outdated request without the new value within a coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestAddedBoolean.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent1 + "add a new optional field, and " +
      "be able to respond to an outdated request without the new optional value"
  ) {
    runSucceedAssertion(serviceRequestAddedOptionalBoolean.RPCService.bindService[IO], response)(
      _.get(request)
    )
  }
  test(
    testParent1 + "add a new optional field, and " +
      "be able to respond to an outdated request without the new optional value within a coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestAddedOptionalBoolean.RPCService.bindService[IO],
      responseCoproduct(response)
    )(
      _.getCoproduct(requestCoproduct(request))
    )
  }

  test(
    testParent1 + "add a new item in coproduct, and " +
      "be able to respond to an outdated request with the previous coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestAddedCoproductItem.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }
  test(
    testParent1 + "remove an item in coproduct, and " +
      "be able to respond to an outdated request with the previous coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestRemovedCoproductItem.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

// Ignored
//  test(
//    testParent1 +
//      "be able to respond to an outdated request with the removed valued of the previous coproduct"
//  ) {
//    runSucceedAssertion(
//      serviceRequestRemovedCoproductItem.RPCService.bindService[IO],
//      responseCoproduct(response)
//    )(_.getCoproduct(requestCoproductInt))
//  }

  test(
    testParent1 + "replace an item in coproduct, and " +
      "be able to respond to an outdated request with the previous coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestReplacedCoproductItem.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }
  test(
    testParent1 + "replace an item in coproduct, and " +
      "be able to respond to an outdated request with the previous coproduct AAAAA"
  ) {
    runFailedAssertion(
      serviceRequestReplacedCoproductItem.RPCService.bindService[IO]
    )(_.getCoproduct(requestCoproductString))
  }

  test(
    testParent1 + "remove an existing field, and " +
      "be able to respond to an outdated request with the old value"
  ) {
    runSucceedAssertion(
      serviceRequestDroppedField.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent1 + "remove an existing field, and " +
      "be able to respond to an outdated request with the old value within a coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestDroppedField.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent1 + "replace the type of a field, and " +
      "be able to respond to an outdated request with the previous value"
  ) {
    runSucceedAssertion(
      serviceRequestReplacedType.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent1 + "replace the type of a field, and " +
      "be able to respond to an outdated request with the previous value within a coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestReplacedType.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent1 + "rename an existing field, and " +
      "be able to respond to an outdated request with the previous name"
  ) {
    runSucceedAssertion(
      serviceRequestRenamedField.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent1 + "rename an existing field, and " +
      "be able to respond to an outdated request with the previous name within a coproduct"
  ) {
    runSucceedAssertion(
      serviceRequestRenamedField.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  val testParent2 = "An AvroWithSchema service with an updated response model"

  test(
    testParent2 + "add a new non-optional field, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseAddedBoolean.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent2 + "add a new non-optional field, and " +
      "be able to provide a compatible response within a coproduct"
  ) {
    runSucceedAssertion(
      serviceResponseAddedBoolean.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "add a new item in a coproduct, and " +
      "be able to provide a compatible response within a coproduct"
  ) {
    runSucceedAssertion(
      serviceResponseAddedBooleanCoproduct.RPCService.bindService[IO],
      ResponseCoproduct(Coproduct[Int :+: String :+: Response :+: CNil](0))
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "remove an item in a coproduct, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseRemovedIntCoproduct.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "replace an item in a coproduct, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseReplacedCoproduct.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "change the type of a field, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseReplacedType.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent2 + "change the type of a field, and " +
      "be able to provide a compatible response within a coproduct"
  ) {
    runSucceedAssertion(
      serviceResponseReplacedType.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "rename a field, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseRenamedField.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent2 + "rename a field, and " +
      "be able to provide a compatible response within a coproduct"
  ) {
    runSucceedAssertion(
      serviceResponseRenamedField.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

  test(
    testParent2 + "drop a field, and " +
      "be able to provide a compatible response"
  ) {
    runSucceedAssertion(
      serviceResponseDroppedField.RPCService.bindService[IO],
      response
    )(_.get(request))
  }
  test(
    testParent2 + "drop a field, and " +
      "be able to provide a compatible response within a coproduct"
  ) {
    runSucceedAssertion(
      serviceResponseDroppedField.RPCService.bindService[IO],
      responseCoproduct(response)
    )(_.getCoproduct(requestCoproduct(request)))
  }

}
