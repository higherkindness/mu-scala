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

package higherkindness.mu.tests.rpc.metrics

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.tests.rpc.metrics.{MetricsTestService, Request, Response}
import io.grpc.MethodDescriptor.MethodType
import higherkindness.mu.rpc.testing.servers._

object Services {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  val error: Throwable = new RuntimeException("BOOM!")

  val serviceOp1Info: GrpcMethodInfo =
    GrpcMethodInfo(
      "higherkindness.mu.tests.rpc.MetricsTestService",
      "higherkindness.mu.tests.rpc.MetricsTestService/serviceOp1",
      "serviceOp1",
      MethodType.UNARY
    )

  val serviceOp2Info: GrpcMethodInfo =
    GrpcMethodInfo(
      "higherkindness.mu.tests.rpc.MetricsTestService",
      "higherkindness.mu.tests.rpc.MetricsTestService/serviceOp2",
      "serviceOp2",
      MethodType.UNARY
    )

  val protoRPCServiceImpl: MetricsTestService[IO] = new MetricsTestService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO(Response())
    def serviceOp2(r: Request): IO[Response] = IO(Response())
  }

  val protoRPCServiceErrorImpl: MetricsTestService[IO] = new MetricsTestService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO.raiseError(error)
    def serviceOp2(r: Request): IO[Response] = IO.raiseError(error)
  }

  def createClient(sc: ServerChannel): Resource[IO, MetricsTestService[IO]] =
    MetricsTestService.clientFromChannel[IO](IO.pure(sc.channel))

}
