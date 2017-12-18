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
package server.handlers

import freestyle.rpc.server.RpcServerTestSuite
import io.grpc.{Server, ServerServiceDefinition}

import scala.concurrent.duration.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

class GrpcServerHandlerTests extends RpcServerTestSuite {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  import implicits._

  val handler: GrpcServerHandler[Future] = new GrpcServerHandler[Future]

  "GrpcServer.Handler" should {

    "allow to start a GrpcServer" in {

      runKFuture(handler.start, serverMock) shouldBe serverCopyMock
      (serverMock.start _: () => Server).verify().once()

    }

    "allow to get the port where server is running" in {

      runKFuture(handler.getPort, serverMock) shouldBe port
      (serverMock.getPort _: () => Int).verify().once()

    }

    "allow to get the services running under the Server instance" in {

      runKFuture(handler.getServices, serverMock) shouldBe serviceList
      (serverMock.getServices _: () => java.util.List[ServerServiceDefinition]).verify().once()

    }

    "allow to get the immutable services running under the Server instance" in {

      runKFuture(handler.getImmutableServices, serverMock) shouldBe immutableServiceList
      (serverMock.getImmutableServices _: () => java.util.List[ServerServiceDefinition])
        .verify()
        .once()

    }

    "allow to get the mutable services running under the Server instance" in {

      runKFuture(handler.getMutableServices, serverMock) shouldBe mutableServiceList
      (serverMock.getMutableServices _: () => java.util.List[ServerServiceDefinition])
        .verify()
        .once()

    }

    "allow to stop a started GrpcServer" in {

      runKFuture(handler.shutdown, serverMock) shouldBe serverCopyMock
      (serverMock.shutdown _: () => Server).verify().once()

    }

    "allow to stop immediately a started GrpcServer" in {

      runKFuture(handler.shutdownNow, serverMock) shouldBe serverCopyMock
      (serverMock.shutdownNow _: () => Server).verify().once()

    }

    "allow to ask whether a Server is shutdown" in {

      runKFuture(handler.isShutdown, serverMock) shouldBe b
      (serverMock.isShutdown _: () => Boolean).verify().once()

    }

    "allow to ask whether a Server instance has been terminated" in {

      runKFuture(handler.isTerminated, serverMock) shouldBe b
      (serverMock.isTerminated _: () => Boolean).verify().once()

    }

    "allow to terminate after a certain timeout is reached" in {

      runKFuture(handler.awaitTerminationTimeout(timeout, timeoutUnit), serverMock) shouldBe b
      (serverMock.awaitTermination(_: Long, _: TimeUnit)).verify(timeout, timeoutUnit).once()

    }

    "allow stopping a started GrpcServer" in {

      runKFuture(handler.awaitTermination, serverMock) shouldBe unit
      (serverMock.awaitTermination _: () => Unit).verify().once()

    }

  }

}
