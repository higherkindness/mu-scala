/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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
package server.handlers

import cats.effect.IO
import higherkindness.mu.rpc.common.SC
import munit.CatsEffectSuite

import scala.concurrent.ExecutionContext

class GrpcServerHandlerTests extends CatsEffectSuite {

  val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val handler = GrpcServerHandler[IO]

  import higherkindness.mu.rpc.server.TestData._

  test("GrpcServer.Handler should allow to start a GrpcServer") {
    handler.start.run(serverMock).attempt.map(_.isRight).assert
  }

  test(
    "GrpcServer.Handler should " +
      "allow to get the port where server is running"
  ) {
    handler.getPort.run(serverMock).assertEquals(SC.port)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to get the services running under the Server instance"
  ) {
    handler.getServices.run(serverMock).assertEquals(serviceList)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to get the immutable services running under the Server instance"
  ) {
    handler.getImmutableServices.run(serverMock).assertEquals(immutableServiceList)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to get the mutable services running under the Server instance"
  ) {
    handler.getMutableServices.run(serverMock).assertEquals(mutableServiceList)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to stop a started GrpcServer"
  ) {
    handler.shutdown.run(serverMock).attempt.map(_.isRight).assert
  }

  test(
    "GrpcServer.Handler should " +
      "allow to stop immediately a started GrpcServer"
  ) {
    handler.shutdownNow.run(serverMock).attempt.map(_.isRight).assert
  }

  test(
    "GrpcServer.Handler should " +
      "allow to ask whether a Server is shutdown"
  ) {
    handler.isShutdown.run(serverMock).assertEquals(b)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to ask whether a Server instance has been terminated"
  ) {
    handler.isTerminated.run(serverMock).assertEquals(b)
  }

  test(
    "GrpcServer.Handler should " +
      "allow to terminate after a certain timeout is reached"
  ) {
    handler
      .awaitTerminationTimeout(timeout, timeoutUnit)
      .run(serverMock)
      .assertEquals(b)
  }

  test(
    "GrpcServer.Handler should " +
      "allow stopping a started GrpcServer"
  ) {
    handler.awaitTermination.run(serverMock).assertEquals(unit)
  }

}
