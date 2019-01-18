/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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
package channel.metrics

import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.syntax.apply._
import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.protocol._
import higherkindness.mu.rpc.testing.servers._
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status

class MonitoringChannelInterceptorTests extends RpcBaseTestSuite {

  import services._

  val myClassifier: Option[String] = Some("MyClassifier")

  "MonitoringChannelInterceptor" should {

    "work for unary RPC metrics" in {

      def makeCalls(metricsOps: MetricsOps[IO]): IO[Response] =
        withServerChannel[IO](
          service = ProtoRPCService.bindService[IO],
          clientInterceptor = Some(MetricsChannelInterceptor(metricsOps, myClassifier)))
          .flatMap(createClient)
          .use(_.unary(Request()))

      val (incArgs, sentArgs, recArgs, headersArgs, totalArgs, decArgs) = (for {
        metricsOps <- MetricsOpsRegister.build
        _          <- makeCalls(metricsOps)
        arg1       <- metricsOps.getIncreaseActiveCallsList
        arg2       <- metricsOps.getRecordMessageSentList
        arg3       <- metricsOps.getRecordMessageReceivedList
        arg4       <- metricsOps.getRecordHeadersTimeList
        arg5       <- metricsOps.getRecordTotalTimeList
        arg6       <- metricsOps.getDecreaseActiveCallsList
      } yield (arg1, arg2, arg3, arg4, arg5, arg6)).unsafeRunSync()

      incArgs shouldBe List((grpcMethodInfo, myClassifier))
      sentArgs shouldBe List((grpcMethodInfo, myClassifier))
      recArgs shouldBe List((grpcMethodInfo, myClassifier))
      decArgs shouldBe List((grpcMethodInfo, myClassifier))
      headersArgs.map(_._1) shouldBe List(grpcMethodInfo)
      headersArgs.map(_._2).forall(_ > 0) shouldBe true
      headersArgs.map(_._3) shouldBe List(myClassifier)
      totalArgs.map(_._1) shouldBe List(grpcMethodInfo)
      totalArgs.map(_._2) shouldBe List(Status.OK)
      totalArgs.map(_._3).forall(_ > 0) shouldBe true
      totalArgs.map(_._4) shouldBe List(myClassifier)
    }

  }

}

object services {

  implicit val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val timer: cats.effect.Timer[cats.effect.IO]     = cats.effect.IO.timer(EC)
  implicit val cs: cats.effect.ContextShift[cats.effect.IO] = cats.effect.IO.contextShift(EC)

  final case class Request()
  final case class Response()

  @service(Protobuf) trait ProtoRPCService[F[_]] {
    def unary(r: Request): F[Response]
  }

  val grpcMethodInfo: GrpcMethodInfo =
    GrpcMethodInfo("ProtoRPCService", "ProtoRPCService/unary", "unary", MethodType.UNARY)

  implicit val protoRPCServiceImpl: ProtoRPCService[IO] = new ProtoRPCService[IO] {
    override def unary(r: Request): IO[Response] = IO(Response())
  }

  type Params1 = List[(GrpcMethodInfo, Option[String])]
  type Params2 = List[(GrpcMethodInfo, Long, Option[String])]
  type Params3 = List[(GrpcMethodInfo, Status, Long, Option[String])]

  case class MetricsOpsRegister(
      paramsRef1: Ref[IO, Params1],
      paramsRef2: Ref[IO, Params1],
      paramsRef3: Ref[IO, Params1],
      paramsRef4: Ref[IO, Params1],
      paramsRef5: Ref[IO, Params2],
      paramsRef6: Ref[IO, Params3],
  ) extends MetricsOps[IO] {

    def increaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): IO[Unit] =
      paramsRef1.update(_ :+ ((methodInfo, classifier)))
    def decreaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): IO[Unit] =
      paramsRef2.update(_ :+ ((methodInfo, classifier)))
    def recordMessageSent(methodInfo: GrpcMethodInfo, classifier: Option[String]): IO[Unit] =
      paramsRef3.update(_ :+ ((methodInfo, classifier)))
    def recordMessageReceived(methodInfo: GrpcMethodInfo, classifier: Option[String]): IO[Unit] =
      paramsRef4.update(_ :+ ((methodInfo, classifier)))
    def recordHeadersTime(
        methodInfo: GrpcMethodInfo,
        elapsed: Long,
        classifier: Option[String]): IO[Unit] =
      paramsRef5.update(_ :+ ((methodInfo, elapsed, classifier)))
    def recordTotalTime(
        methodInfo: GrpcMethodInfo,
        status: Status,
        elapsed: Long,
        classifier: Option[String]): IO[Unit] =
      paramsRef6.update(_ :+ ((methodInfo, status, elapsed, classifier)))

    def getIncreaseActiveCallsList: IO[Params1]   = paramsRef1.get
    def getDecreaseActiveCallsList: IO[Params1]   = paramsRef2.get
    def getRecordMessageSentList: IO[Params1]     = paramsRef3.get
    def getRecordMessageReceivedList: IO[Params1] = paramsRef4.get
    def getRecordHeadersTimeList: IO[Params2]     = paramsRef5.get
    def getRecordTotalTimeList: IO[Params3]       = paramsRef6.get

  }

  object MetricsOpsRegister {
    def build: IO[MetricsOpsRegister] =
      (
        Ref.of[IO, Params1](Nil),
        Ref.of[IO, Params1](Nil),
        Ref.of[IO, Params1](Nil),
        Ref.of[IO, Params1](Nil),
        Ref.of[IO, Params2](Nil),
        Ref.of[IO, Params3](Nil)).mapN(MetricsOpsRegister.apply)
  }

  def createClient(sc: ServerChannel): Resource[IO, ProtoRPCService[IO]] =
    ProtoRPCService.clientFromChannel[IO](IO(sc.channel))

}
