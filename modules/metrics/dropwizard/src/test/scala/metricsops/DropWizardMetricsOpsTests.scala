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

package metricsops

import cats.effect.IO
import cats.implicits._
import com.codahale.metrics.MetricRegistry
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import DropWizardMetricsOps._
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status
import org.scalacheck.Gen.alphaLowerChar
import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll

import scala.collection.JavaConverters._

object DropWizardMetricsOpsTests extends Properties("DropWizardMetrics") {

  val prefix     = "testPrefix"
  val classifier = "classifier"

  def nonEmptyStrGen: Gen[String] = Gen.nonEmptyListOf(alphaLowerChar).map(_.mkString)

  def methodInfoGen: Gen[GrpcMethodInfo] =
    for {
      serviceName    <- nonEmptyStrGen
      fullMethodName <- nonEmptyStrGen
      methodName     <- nonEmptyStrGen
      methodType <- Gen.oneOf(
        Seq(
          MethodType.BIDI_STREAMING,
          MethodType.CLIENT_STREAMING,
          MethodType.SERVER_STREAMING,
          MethodType.UNARY,
          MethodType.UNKNOWN))
    } yield
      GrpcMethodInfo(
        serviceName,
        fullMethodName,
        methodName,
        methodType
      )

  def statusGen: Gen[Status] = Gen.oneOf(
    Status.ABORTED,
    Status.ALREADY_EXISTS,
    Status.CANCELLED,
    Status.DATA_LOSS,
    Status.DEADLINE_EXCEEDED,
    Status.FAILED_PRECONDITION,
    Status.INTERNAL,
    Status.INVALID_ARGUMENT,
    Status.NOT_FOUND,
    Status.OK,
    Status.OUT_OF_RANGE,
    Status.PERMISSION_DENIED,
    Status.RESOURCE_EXHAUSTED,
    Status.UNAUTHENTICATED,
    Status.UNAVAILABLE,
    Status.UNIMPLEMENTED,
    Status.UNKNOWN
  )

  def methodTypeName(methodType: MethodType): String = methodType match {
    case MethodType.UNARY            => "unary"
    case MethodType.CLIENT_STREAMING => "client-streaming"
    case MethodType.SERVER_STREAMING => "server-streaming"
    case MethodType.BIDI_STREAMING   => "bidi-streaming"
    case MethodType.UNKNOWN          => "unknown"
  }

  def performAndCheckMetrics(
      methodInfo: GrpcMethodInfo,
      numberOfCalls: Int,
      expectedCount: Int,
      registry: MetricRegistry,
      gaugeName: String,
      gaugeType: GaugeType,
      f: () => IO[Unit],
      status: Option[Status] = None): Boolean = {

    (1 to numberOfCalls).toList
      .map { _ =>
        f()
      }
      .sequence
      .unsafeRunSync()

    checkMetrics(expectedCount, registry, gaugeName, gaugeType)
  }

  def checkMetrics(
      expectedCount: Int,
      registry: MetricRegistry,
      gaugeName: String,
      gaugeType: GaugeType
  ): Boolean =
    gaugeType match {
      case Timer =>
        registry.getTimers().asScala.contains(gaugeName) && registry
          .timer(gaugeName)
          .getCount == expectedCount
      case Counter =>
        registry
          .getCounters()
          .asScala
          .contains(gaugeName) && registry
          .counter(gaugeName)
          .getCount == expectedCount
      case _ => false
    }

  property("creates and updates counter when registering an active call") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry        = new MetricRegistry()
        val metrics         = DropWizardMetricsOps[IO](registry, prefix)
        val activeCallsName = s"$prefix.$classifier.active.calls"

        performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          activeCallsName,
          Counter,
          () => metrics.increaseActiveCalls(methodInfo, Some(classifier))
        ) && performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          0,
          registry,
          activeCallsName,
          Counter,
          () => metrics.decreaseActiveCalls(methodInfo, Some(classifier))
        )
    }

  property("creates and updates counter when registering a sent message") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)
        val messagesSentName =
          s"$prefix.$classifier.${methodInfo.serviceName}.${methodInfo.methodName}.messages-sent"

        performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          messagesSentName,
          Counter,
          () => metrics.recordMessageSent(methodInfo, Some(classifier))
        )
    }

  property("creates and updates counter when registering a received message") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)
        val messagesReceivedName =
          s"$prefix.$classifier.${methodInfo.serviceName}.${methodInfo.methodName}.messages-received"

        performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          messagesReceivedName,
          Counter,
          () => metrics.recordMessageReceived(methodInfo, Some(classifier))
        )
    }

  property("creates and updates timer for headers time") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10), Gen.chooseNum(100, 1000)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, elapsed: Int) =>
        val registry    = new MetricRegistry()
        val metrics     = DropWizardMetricsOps[IO](registry, prefix)
        val headersName = s"$prefix.$classifier.calls.header"

        performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          headersName,
          Timer,
          () => metrics.recordHeadersTime(methodInfo, elapsed.toLong, Some(classifier))
        )
    }

  property("creates and updates timer for total time") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10), statusGen) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, status: Status) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        performAndCheckMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          s"$prefix.$classifier.calls.total",
          Timer,
          () => metrics.recordTotalTime(methodInfo, status, 1L, Some(classifier))
        ) && checkMetrics(
          numberOfCalls,
          registry,
          s"$prefix.$classifier.${methodTypeDescription(methodInfo)}.calls",
          Timer
        ) && checkMetrics(
          numberOfCalls,
          registry,
          s"$prefix.$classifier.${statusDescription(MetricsOps.grpcStatusFromRawStatus(status))}.calls",
          Timer
        )
    }

}
