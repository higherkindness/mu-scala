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

package higherkindness.mu.rpc.prometheus

import cats.effect.IO
import cats.implicits._
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import higherkindness.mu.rpc.internal.metrics.MetricsOps
import higherkindness.mu.rpc.internal.metrics.MetricsOps._
import higherkindness.mu.rpc.internal.metrics.MetricsOpsGenerators.{methodInfoGen, statusGen}
import io.grpc.Status
import io.prometheus.client.CollectorRegistry
import org.scalacheck.Prop.forAllNoShrink
import org.scalacheck.{Gen, Properties}

class PrometheusMetricsTests extends Properties("DropWizardMetrics") {

  val prefix     = "testPrefix"
  val classifier = "classifier"

  def performAndCheckMetrics(
      methodInfo: GrpcMethodInfo,
      numberOfCalls: Int,
      expectedValue: Double,
      registry: CollectorRegistry,
      gaugeName: String,
      labelNames: List[String],
      labelValues: List[String],
      op: IO[Unit],
      status: Option[Status] = None): IO[Boolean] =
    (1 to numberOfCalls).toList
      .map(_ => op)
      .sequence_
      .map(_ => checkMetrics(expectedValue, registry, gaugeName, labelNames, labelValues))

  def checkMetrics(
      expectedValue: Double,
      registry: CollectorRegistry,
      metricName: String,
      labelNames: List[String],
      labelValues: List[String]
  ): Boolean =
    registry.getSampleValue(metricName, labelNames.toArray, labelValues.toArray) == expectedValue

  property("creates and updates counter when registering an active call") =
    forAllNoShrink(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry   = new CollectorRegistry()
        val metricName = s"${prefix}_active_calls"

        (for {
          metrics <- PrometheusMetrics[IO](registry, prefix, Some(classifier))
          op1 <- performAndCheckMetrics(
            methodInfo,
            numberOfCalls,
            numberOfCalls.toDouble,
            registry,
            metricName,
            List("classifier"),
            List(classifier),
            metrics.increaseActiveCalls(methodInfo, Some(classifier))
          )
          op2 <- performAndCheckMetrics(
            methodInfo,
            numberOfCalls,
            expectedValue = 0L,
            registry,
            metricName,
            List("classifier"),
            List(classifier),
            metrics.decreaseActiveCalls(methodInfo, Some(classifier))
          )
        } yield op1 && op2).unsafeRunSync()
    }

  property("creates and updates counter when registering a sent message") =
    forAllNoShrink(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry   = new CollectorRegistry()
        val metricName = s"${prefix}_messages_sent"

        (for {
          metrics <- PrometheusMetrics[IO](registry, prefix, Some(classifier))
          op1 <- performAndCheckMetrics(
            methodInfo,
            numberOfCalls,
            numberOfCalls.toDouble,
            registry,
            metricName,
            List("classifier", "service", "method"),
            List(classifier, methodInfo.serviceName, methodInfo.methodName),
            metrics.recordMessageSent(methodInfo, Some(classifier))
          )
        } yield op1).unsafeRunSync()
    }

  property("creates and updates counter when registering a received message") =
    forAllNoShrink(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry   = new CollectorRegistry()
        val metricName = s"${prefix}_messages_received"

        (for {
          metrics <- PrometheusMetrics[IO](registry, prefix, Some(classifier))
          op1 <- performAndCheckMetrics(
            methodInfo,
            numberOfCalls,
            numberOfCalls.toDouble,
            registry,
            metricName,
            List("classifier", "service", "method"),
            List(classifier, methodInfo.serviceName, methodInfo.methodName),
            metrics.recordMessageReceived(methodInfo, Some(classifier))
          )
        } yield op1).unsafeRunSync()
    }

  property("creates and updates timer for headers time") =
    forAllNoShrink(methodInfoGen, Gen.chooseNum[Int](1, 10), Gen.chooseNum(100, 1000)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, elapsed: Int) =>
        val registry   = new CollectorRegistry()
        val metricName = s"${prefix}_calls_header"

        (for {
          metrics <- PrometheusMetrics[IO](registry, prefix, Some(classifier))
          op1 <- performAndCheckMetrics(
            methodInfo,
            numberOfCalls,
            (elapsed * numberOfCalls).toDouble,
            registry,
            metricName,
            List("classifier"),
            List(classifier),
            metrics.recordHeadersTime(methodInfo, elapsed.toLong, Some(classifier))
          )
        } yield op1).unsafeRunSync()
    }

  property("creates and updates timer for total time") =
    forAllNoShrink(methodInfoGen, Gen.chooseNum[Int](1, 10), statusGen) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, status: Status) =>
        val registry              = new CollectorRegistry()
        val metricNameTotal       = s"${prefix}_calls_total"
        val metricNameTotalMethod = s"${prefix}_calls_by_method"
        val metricNameTotalStatus = s"${prefix}_calls_by_status"

        (for {
          metrics <- PrometheusMetrics[IO](registry, prefix, Some(classifier))
          op1 <- (1 to numberOfCalls).toList
            .map(_ => metrics.recordTotalTime(methodInfo, status, 1L, Some(classifier)))
            .sequence_
            .map { _ =>
              checkMetrics(
                numberOfCalls.toDouble,
                registry,
                metricNameTotal,
                List("classifier"),
                List(classifier)) &&
              checkMetrics(
                numberOfCalls.toDouble,
                registry,
                metricNameTotalMethod,
                List("classifier", "method"),
                List(classifier, methodTypeDescription(methodInfo))) &&
              checkMetrics(
                numberOfCalls.toDouble,
                registry,
                metricNameTotalStatus,
                List("classifier", "status"),
                List(classifier, statusDescription(MetricsOps.grpcStatusFromRawStatus(status)))
              )
            }
        } yield op1).unsafeRunSync()
    }
}
