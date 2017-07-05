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
package server

import java.util.concurrent.TimeUnit

import cats.{~>, Id}
import cats.data.Kleisli
import io.grpc.{Server, ServerServiceDefinition}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, TimeUnit}
import scala.concurrent.{Await, Future}

trait RpcTestSuite extends WordSpec with Matchers with OneInstancePerTest with MockFactory {

  def runK[F[_], A, B](kleisli: Kleisli[F, A, B], v: A): F[B] =
    kleisli.run(v)

  def runKFuture[A, B](kleisli: Kleisli[Future, A, B], v: A): B =
    Await.result(kleisli.run(v), Duration.Inf)

  trait DummyData {

    val serverMock: Server                                  = stub[Server]
    val serverCopyMock: Server                              = stub[Server]
    val port: Int                                           = 42
    val timeout: Long                                       = 1l
    val timeoutUnit: TimeUnit                               = TimeUnit.MINUTES
    val b: Boolean                                          = true
    val unit: Unit                                          = ()
    val sd1: ServerServiceDefinition                        = ServerServiceDefinition.builder("s1").build()
    val sd2: ServerServiceDefinition                        = ServerServiceDefinition.builder("s2").build()
    val serviceList: List[ServerServiceDefinition]          = List(sd1, sd2)
    val immutableServiceList: List[ServerServiceDefinition] = List(sd1)
    val mutableServiceList: List[ServerServiceDefinition]   = List(sd2)

    (serverMock.start _).when().returns(serverCopyMock)
    (serverMock.getPort _).when().returns(port)
    (serverMock.getServices _).when().returns(serviceList.asJava)
    (serverMock.getImmutableServices _).when().returns(immutableServiceList.asJava)
    (serverMock.getMutableServices _).when().returns(mutableServiceList.asJava)
    (serverMock.shutdown _).when().returns(serverCopyMock)
    (serverMock.shutdownNow _).when().returns(serverCopyMock)
    (serverMock.isShutdown _).when().returns(b)
    (serverMock.isTerminated _).when().returns(b)
    (serverMock.awaitTermination(_: Long, _: TimeUnit)).when(timeout, timeoutUnit).returns(b)
    (serverMock.awaitTermination _).when().returns(unit)
  }

  object implicits extends DummyData {

    implicit val grpcServerHandler: GrpcServer.Op ~> Id = new (GrpcServer.Op ~> Id) {
      import GrpcServer._
      override def apply[A](fa: GrpcServer.Op[A]): Id[A] = fa match {
        case StartOP()                                => serverMock.start()
        case GetPortOP()                              => serverMock.getPort
        case GetServicesOP()                          => serverMock.getServices.asScala.toList
        case GetImmutableServicesOP()                 => serverMock.getImmutableServices.asScala.toList
        case GetMutableServicesOP()                   => serverMock.getMutableServices.asScala.toList
        case ShutdownOP()                             => serverMock.shutdown()
        case ShutdownNowOP()                          => serverMock.shutdownNow()
        case IsShutdownOP()                           => serverMock.isShutdown
        case IsTerminatedOP()                         => serverMock.isTerminated
        case AwaitTerminationTimeoutOP(timeout, unit) => serverMock.awaitTermination(timeout, unit)
        case AwaitTerminationOP()                     => serverMock.awaitTermination()
      }
    }
  }
}
