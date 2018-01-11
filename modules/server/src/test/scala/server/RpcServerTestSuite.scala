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
package server

import java.util.concurrent.TimeUnit

import cats.Applicative
import freestyle.rpc.common.{RpcBaseTestSuite, SC}
import io.grpc.{Server, ServerServiceDefinition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.TimeUnit

trait RpcServerTestSuite extends RpcBaseTestSuite {

  trait DummyData {

    val serverMock: Server                                  = stub[Server]
    val serverCopyMock: Server                              = stub[Server]
    val timeout: Long                                       = 1l
    val timeoutUnit: TimeUnit                               = TimeUnit.MINUTES
    val b: Boolean                                          = true
    val unit: Unit                                          = ()
    val sd1: ServerServiceDefinition                        = ServerServiceDefinition.builder("s1").build()
    val sd2: ServerServiceDefinition                        = ServerServiceDefinition.builder("s2").build()
    val serviceList: List[ServerServiceDefinition]          = List(sd1, sd2)
    val immutableServiceList: List[ServerServiceDefinition] = List(sd1)
    val mutableServiceList: List[ServerServiceDefinition]   = List(sd2)

    (serverMock.start _: () => Server).when().returns(serverCopyMock)
    (serverMock.getPort _: () => Int).when().returns(SC.port)
    (serverMock.getServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(serviceList.asJava)
    (serverMock.getImmutableServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(immutableServiceList.asJava)
    (serverMock.getMutableServices _: () => java.util.List[ServerServiceDefinition])
      .when()
      .returns(mutableServiceList.asJava)
    (serverMock.shutdown _: () => Server).when().returns(serverCopyMock)
    (serverMock.shutdownNow _: () => Server).when().returns(serverCopyMock)
    (serverMock.isShutdown _: () => Boolean).when().returns(b)
    (serverMock.isTerminated _: () => Boolean).when().returns(b)
    (serverMock.awaitTermination(_: Long, _: TimeUnit)).when(timeout, timeoutUnit).returns(b)
    (serverMock.awaitTermination _: () => Unit).when().returns(unit)
  }

  object implicits extends Helpers with DummyData {

    def grpcServerHandlerTests[F[_]](implicit F: Applicative[F]): GrpcServer[F] = {
      new GrpcServer[F] {

        def start(): F[Server] = F.pure(serverMock.start())

        def getPort: F[Int] = F.pure(serverMock.getPort)

        def getServices: F[List[ServerServiceDefinition]] =
          F.pure(serverMock.getServices.asScala.toList)

        def getImmutableServices: F[List[ServerServiceDefinition]] =
          F.pure(serverMock.getImmutableServices.asScala.toList)

        def getMutableServices: F[List[ServerServiceDefinition]] =
          F.pure(serverMock.getMutableServices.asScala.toList)

        def shutdown(): F[Server] = F.pure(serverMock.shutdown())

        def shutdownNow(): F[Server] = F.pure(serverMock.shutdownNow())

        def isShutdown: F[Boolean] = F.pure(serverMock.isShutdown)

        def isTerminated: F[Boolean] = F.pure(serverMock.isTerminated)

        def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): F[Boolean] =
          F.pure(serverMock.awaitTermination(timeout, unit))

        def awaitTermination(): F[Unit] = F.pure(serverMock.awaitTermination())

      }
    }
  }
}
