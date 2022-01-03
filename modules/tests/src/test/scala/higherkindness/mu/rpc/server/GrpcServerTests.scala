/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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
package server

import java.util.concurrent.TimeUnit

import cats.Apply
import cats.syntax.all._
import higherkindness.mu.rpc.common.SC
import cats.effect.{IO, Sync}
import io.grpc.ServerServiceDefinition
import munit.CatsEffectSuite

import scala.jdk.CollectionConverters._

class GrpcServerTests extends CatsEffectSuite {

  import TestData._

  type Result = (
      Boolean,
      Unit,
      Int,
      List[ServerServiceDefinition],
      List[ServerServiceDefinition],
      List[ServerServiceDefinition],
      Unit,
      Unit,
      Boolean,
      Boolean,
      Boolean,
      Unit
  )

  test("GrpcServer should behaves as expected") {

    def program[F[_]: Apply](APP: GrpcServer[F]): F[Result] = {

      import APP._

      val a = isShutdown
      val b = start
      val c = getPort
      val d = getServices
      val e = getImmutableServices
      val f = getMutableServices
      val g = shutdown
      val h = shutdownNow
      val i = isShutdown
      val j = isTerminated
      val k = awaitTerminationTimeout(timeout, timeoutUnit)
      val l = awaitTermination

      (a, b, c, d, e, f, g, h, i, j, k, l).tupled
    }

    program[IO](grpcServerHandlerTests).map(
      assertEquals(
        _,
        (
          b,
          (),
          SC.port,
          serviceList,
          immutableServiceList,
          mutableServiceList,
          (),
          (),
          b,
          b,
          b,
          unit
        ): Result
      )
    )
  }

  def grpcServerHandlerTests[F[_]](implicit F: Sync[F]): GrpcServer[F] = {
    new GrpcServer[F] {
      def start: F[Unit] = F.pure(serverMock.start).void

      def getPort: F[Int] = F.pure(serverMock.getPort)

      def getServices: F[List[ServerServiceDefinition]] =
        F.pure(serverMock.getServices.asScala.toList)

      def getImmutableServices: F[List[ServerServiceDefinition]] =
        F.pure(serverMock.getImmutableServices.asScala.toList)

      def getMutableServices: F[List[ServerServiceDefinition]] =
        F.pure(serverMock.getMutableServices.asScala.toList)

      def shutdown: F[Unit] = F.pure(serverMock.shutdown()).void

      def shutdownNow: F[Unit] = F.pure(serverMock.shutdownNow()).void

      def isShutdown: F[Boolean] = F.pure(serverMock.isShutdown)

      def isTerminated: F[Boolean] = F.pure(serverMock.isTerminated)

      def awaitTerminationTimeout(timeout: Long, unit: TimeUnit): F[Boolean] =
        F.pure(serverMock.awaitTermination(timeout, unit))

      def awaitTermination: F[Unit] = F.pure(serverMock.awaitTermination())

    }
  }

}
