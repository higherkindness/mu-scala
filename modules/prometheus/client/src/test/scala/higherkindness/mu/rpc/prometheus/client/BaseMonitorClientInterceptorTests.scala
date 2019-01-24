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
package prometheus
package client

import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.protocol.Utils.client.MyRPCClient
import io.prometheus.client.{Collector, CollectorRegistry}
import higherkindness.mu.rpc.interceptors.metrics._
import io.prometheus.client.Collector.MetricFamilySamples
import org.scalatest.exceptions.TestFailedException
import org.scalatest.Assertion
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

abstract class BaseMonitorClientInterceptorTests extends RpcBaseTestSuite {

  import higherkindness.mu.rpc.protocol.Utils.database._
  import higherkindness.mu.rpc.prometheus.shared.RegistryHelper._

  def name: String
  def namespace: Option[String]
  def defaultClientRuntime: InterceptorsRuntime
  def allMetricsClientRuntime: InterceptorsRuntime
  def clientRuntimeWithNonDefaultBuckets(buckets: Vector[Double]): InterceptorsRuntime

  def beASampleMetric(
      labels: List[String],
      minValue: Double = 0d,
      maxValue: Double = 1d): Matcher[Option[MetricFamilySamples.Sample]] =
    new Matcher[Option[MetricFamilySamples.Sample]] {
      def apply(maybe: Option[MetricFamilySamples.Sample]): MatchResult = MatchResult(
        maybe.forall { s =>
          s.value should be >= minValue
          s.value should be <= maxValue
          s.labelValues.asScala.toList.sorted shouldBe labels.sorted
          s.value >= minValue && s.value <= maxValue && s.labelValues.asScala.toList.sorted == labels.sorted
        },
        "Incorrect metric",
        "Valid metric"
      )
    }

  @tailrec
  private[this] def checkWithRetry(assertionF: () => Assertion)(
      step: Int = 0,
      maxRetries: Int = 10,
      sleepMillis: Long = 200)(implicit CR: CollectorRegistry): Assertion =
    try {
      assertionF()
    } catch {
      case e: TestFailedException =>
        if (step == maxRetries) throw e
        else {
          Thread.sleep(sleepMillis)
          checkWithRetry(assertionF)(step + 1, maxRetries, sleepMillis)
        }
    }

  s"MonitorClientInterceptor for $name" should {

    "work for unary RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        val startedTotal: Double = extractMetricValue(clientMetricRpcStarted(namespace))
        startedTotal should be >= 0d
        startedTotal should be <= 1d
        findRecordedMetricOrThrow(clientMetricStreamMessagesReceived(namespace)).samples shouldBe empty
        findRecordedMetricOrThrow(clientMetricStreamMessagesSent(namespace)).samples shouldBe empty

        val handledSamples =
          findRecordedMetricOrThrow(clientMetricRpcCompleted(namespace)).samples.asScala.toList
        handledSamples.size shouldBe 1
        handledSamples.headOption should beASampleMetric(
          List("AvroRPCService", "OK", "UNARY", "unary"))
      }

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- clientProgram[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

    "work for client streaming RPC metrics" in {

      ignoreOnTravis("TODO: restore once https://github.com/higherkindness/mu/issues/168 is fixed")

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      def clientProgram2[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(List(c1), i)

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      def check1(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {

        val startedTotal: Double = extractMetricValue(clientMetricRpcStarted(namespace))

        startedTotal should be >= 0d
        startedTotal should be <= 1d

        val msgSentTotal: Double = extractMetricValue(clientMetricStreamMessagesSent(namespace))
        msgSentTotal should be >= 0d
        msgSentTotal should be <= 2d

        checkWithRetry(() =>
          findRecordedMetricOrThrow(clientMetricStreamMessagesReceived(namespace)).samples shouldBe empty)()
      }

      def check2(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {

        val msgSentTotal2: Double = extractMetricValue(clientMetricStreamMessagesSent(namespace))
        msgSentTotal2 should be >= 0d
        msgSentTotal2 should be <= 3d

        def completedAssertion: Assertion = {
          val handledSamples =
            findRecordedMetricOrThrow(clientMetricRpcCompleted(namespace)).samples.asScala.toList
          handledSamples.size shouldBe 1
          handledSamples.headOption should beASampleMetric(
            labels = List("CLIENT_STREAMING", "ProtoRPCService", "clientStreaming", "OK"),
            maxValue = 2d)
        }

        checkWithRetry(() => completedAssertion)()
      }

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- check1
        _ <- clientProgram2[ConcurrentMonad]
        _ <- check2
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

    }

    "work for server streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        val startedTotal: Double = extractMetricValue(clientMetricRpcStarted(namespace))
        val msgReceivedTotal: Double =
          extractMetricValue(clientMetricStreamMessagesReceived(namespace))
        findRecordedMetricOrThrow(clientMetricStreamMessagesSent(namespace)).samples shouldBe empty

        startedTotal should be >= 0d
        startedTotal should be <= 1d
        msgReceivedTotal should be >= 0d
        msgReceivedTotal should be <= 2d

        val handledSamples =
          findRecordedMetricOrThrow(clientMetricRpcCompleted(namespace)).samples.asScala.toList
        handledSamples.size shouldBe 1
        handledSamples.headOption should beASampleMetric(
          List("SERVER_STREAMING", "ProtoRPCService", "serverStreaming", "OK"))
      }

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- clientProgram[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

    "work for bidirectional streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        val startedTotal: Double = extractMetricValue(clientMetricRpcStarted(namespace))
        val msgReceivedTotal: Double =
          extractMetricValue(clientMetricStreamMessagesReceived(namespace))
        val msgSentTotal: Double = extractMetricValue(clientMetricStreamMessagesSent(namespace))

        startedTotal should be >= 0d
        startedTotal should be <= 1d
        msgReceivedTotal should be >= 0d
        msgReceivedTotal should be <= 4d
        msgSentTotal should be >= 0d
        msgSentTotal should be <= 2d

        val handledSamples =
          findRecordedMetricOrThrow(clientMetricRpcCompleted(namespace)).samples.asScala.toList
        handledSamples.size shouldBe 1
        handledSamples.headOption should be

        handledSamples.headOption should beASampleMetric(
          List("AvroRPCService", "BIDI_STREAMING", "OK", "biStreaming"))
      }

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- clientProgram[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()
    }

    "work when no histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        assertion <- suspendM(
          findRecordedMetric(clientMetricCompletedLatencySeconds(namespace)) shouldBe None)
        _ <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

    "work when histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        val metric: Option[Collector.MetricFamilySamples] =
          findRecordedMetric(clientMetricCompletedLatencySeconds(namespace))

        metric shouldBe defined
        metric.fold(true)(_.samples.size > 0) shouldBe true
      }

      val clientRuntime: InterceptorsRuntime = allMetricsClientRuntime
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- clientProgram[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

    "work for different buckets" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val buckets: Vector[Double] = Vector[Double](0.1, 0.2)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        countSamples(
          clientMetricCompletedLatencySeconds(namespace),
          "grpc_client_completed_latency_seconds_bucket") shouldBe (buckets.size + 1)
      }

      val clientRuntime: InterceptorsRuntime = clientRuntimeWithNonDefaultBuckets(buckets)
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- clientProgram[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

    "work when combining multiple calls" in {

      ignoreOnTravis("TODO: restore once https://github.com/higherkindness/mu/issues/168 is fixed")

      def unary[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def clientStreaming[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      def check(implicit CR: CollectorRegistry): ConcurrentMonad[Assertion] = suspendM {
        findRecordedMetricOrThrow(clientMetricRpcStarted(namespace)).samples.size() shouldBe 2
        checkWithRetry(
          () =>
            findRecordedMetricOrThrow(clientMetricRpcCompleted(namespace)).samples
              .size() shouldBe 2)()
      }

      val clientRuntime: InterceptorsRuntime = defaultClientRuntime
      import clientRuntime._

      (for {
        _         <- serverStart[ConcurrentMonad]
        _         <- unary[ConcurrentMonad]
        _         <- clientStreaming[ConcurrentMonad]
        assertion <- check
        _         <- serverStop[ConcurrentMonad]
      } yield assertion).unsafeRunSync()

    }

  }
}
