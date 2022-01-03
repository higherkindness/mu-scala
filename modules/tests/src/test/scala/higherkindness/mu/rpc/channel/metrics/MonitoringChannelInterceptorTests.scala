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

package higherkindness.mu.rpc.channel.metrics

import cats.effect.std.Dispatcher
import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.common.{A => _}
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.{MetricsOps, MetricsOpsRegister}
import higherkindness.mu.rpc.protocol._
import higherkindness.mu.rpc.testing.servers._
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status
import munit.CatsEffectSuite

class MonitoringChannelInterceptorTests extends CatsEffectSuite {

  import services._

  val myClassifier: Option[String] = Some("MyClassifier")

  test("MonitoringChannelInterceptor generate the right metrics with proto") {
    for {
      metricsOps <- MetricsOpsRegister.build
      _          <- makeProtoCalls(metricsOps)(_.serviceOp1(Request()))(protoRPCServiceImpl)
      assertion  <- checkCalls(metricsOps, List(serviceOp1Info))
    } yield assertion
  }

  test(
    "MonitoringChannelInterceptor generate the right metrics when calling multiple methods with proto"
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
    "MonitoringChannelInterceptor generate the right metrics with proto when the server returns an error"
  ) {
    for {
      metricsOps <- MetricsOpsRegister.build
      _          <- makeProtoCalls(metricsOps)(_.serviceOp1(Request()))(protoRPCServiceErrorImpl)
      assertion  <- checkCalls(metricsOps, List(serviceOp1Info), serverError = true)
    } yield assertion
  }

  private[this] def makeProtoCalls[A](metricsOps: MetricsOps[IO])(
      f: ProtoRPCService[IO] => IO[A]
  )(implicit H: ProtoRPCService[IO]): IO[Either[Throwable, A]] = {

    Dispatcher[IO]
      .flatMap { disp =>
        withServerChannel[IO](
          service = ProtoRPCService.bindService[IO],
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

object services {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  final case class Request()
  final case class Response()

  val error: Throwable = new RuntimeException("BOOM!")

  @service(Protobuf) trait ProtoRPCService[F[_]] {
    def serviceOp1(r: Request): F[Response]
    def serviceOp2(r: Request): F[Response]
  }

  val serviceOp1Info: GrpcMethodInfo =
    GrpcMethodInfo("ProtoRPCService", "ProtoRPCService/serviceOp1", "serviceOp1", MethodType.UNARY)

  val serviceOp2Info: GrpcMethodInfo =
    GrpcMethodInfo("ProtoRPCService", "ProtoRPCService/serviceOp2", "serviceOp2", MethodType.UNARY)

  val protoRPCServiceImpl: ProtoRPCService[IO] = new ProtoRPCService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO(Response())
    def serviceOp2(r: Request): IO[Response] = IO(Response())
  }

  val protoRPCServiceErrorImpl: ProtoRPCService[IO] = new ProtoRPCService[IO] {
    def serviceOp1(r: Request): IO[Response] = IO.raiseError(error)
    def serviceOp2(r: Request): IO[Response] = IO.raiseError(error)
  }

  def createClient(sc: ServerChannel): Resource[IO, ProtoRPCService[IO]] =
    ProtoRPCService.clientFromChannel[IO](IO(sc.channel))

}
