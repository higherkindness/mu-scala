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

package higherkindness.mu.tests.rpc.metrics

import cats.effect.IO
import cats.effect.std.Dispatcher
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.channel.metrics.MetricsChannelInterceptor
import higherkindness.mu.rpc.testing.servers.withServerChannel
import io.grpc.Status
import munit.CatsEffectSuite

class MetricsChannelInterceptorTest extends CatsEffectSuite {

  import Services._

  val myClassifier: Option[String] = Some("MyClassifier")

  test("MetricsChannelInterceptor generate the right metrics with proto") {
    for {
      metricsOps <- MetricsOpsRegister.build
      _          <- makeProtoCalls(metricsOps)(_.serviceOp1(Request()))(protoRPCServiceImpl)
      assertion  <- checkCalls(metricsOps, List(serviceOp1Info))
    } yield assertion
  }

  test(
    "MetricsChannelInterceptor generate the right metrics when calling multiple methods with proto"
  ) {
    for {
      metricsOps <- MetricsOpsRegister.build
      _ <- makeProtoCalls(metricsOps) { client =>
        client.serviceOp1(Request()) *> client.serviceOp2(Request())
      }(protoRPCServiceImpl)
      assertion <- checkCalls(metricsOps, List(serviceOp1Info, serviceOp2Info))
    } yield assertion
  }

  test(
    "MetricsChannelInterceptor generate the right metrics with proto when the server returns an error"
  ) {
    for {
      metricsOps <- MetricsOpsRegister.build
      _          <- makeProtoCalls(metricsOps)(_.serviceOp1(Request()))(protoRPCServiceErrorImpl)
      assertion  <- checkCalls(metricsOps, List(serviceOp1Info), serverError = true)
    } yield assertion
  }

  private[this] def makeProtoCalls[A](metricsOps: MetricsOps[IO])(
      f: MetricsTestService[IO] => IO[A]
  )(implicit H: MetricsTestService[IO]): IO[Either[Throwable, A]] = {

    Dispatcher
      .parallel[IO]
      .flatMap { disp =>
        withServerChannel[IO](
          service = MetricsTestService.bindService[IO],
          clientInterceptor = Some(MetricsChannelInterceptor(metricsOps, disp, myClassifier))
        ).flatMap(createClient)
      }
      .use(f(_).attempt)
  }

  private[this] def checkCalls(
      metricsOps: MetricsOpsRegister,
      methodCalls: List[GrpcMethodInfo],
      serverError: Boolean = false
  ): IO[Unit] = {
    for {
      incArgs     <- metricsOps.increaseActiveCallsReg.get
      sentArgs    <- metricsOps.recordMessageSentReg.get
      recArgs     <- metricsOps.recordMessageReceivedReg.get
      headersArgs <- metricsOps.recordHeadersTimeReg.get
      totalArgs   <- metricsOps.recordTotalTimeReg.get
      decArgs     <- metricsOps.decreaseActiveCallsReg.get
    } yield {

      val argList: Set[(GrpcMethodInfo, Option[String])] = methodCalls.map((_, myClassifier)).toSet

      // Increase Active Calls
      assertEquals(incArgs.toSet, argList)
      // Messages Sent
      assertEquals(sentArgs.toSet, argList)
      // Messages Received
      if (!serverError) assertEquals(recArgs.toSet, argList)
      // Decrease Active Calls
      assertEquals(decArgs.toSet, argList)
      // Headers Time
      if (!serverError) {
        assertEquals(headersArgs.map(_._1).toSet, methodCalls.toSet)
        assertEquals(headersArgs.map(_._3).toSet, argList.map(_._2))
      }
      // Total Time
      assertEquals(totalArgs.map(_._1).toSet, methodCalls.toSet)
      if (serverError) {
        assertEquals(
          totalArgs.map(_._2.getCode).toSet,
          List.fill(methodCalls.size)(Status.INTERNAL.getCode).toSet
        )
      } else {
        assertEquals(totalArgs.map(_._2).toSet, List.fill(methodCalls.size)(Status.OK).toSet)
      }
      assertEquals(totalArgs.map(_._4).toSet, argList.map(_._2))
    }
  }
}
