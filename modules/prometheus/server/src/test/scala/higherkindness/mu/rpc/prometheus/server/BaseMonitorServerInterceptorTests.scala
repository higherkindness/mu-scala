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

package higherkindness.mu.rpc
package prometheus
package server

import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.protocol.Utils.client.MyRPCClient
import io.prometheus.client.Collector
import higherkindness.mu.rpc.interceptors.metrics._

import scala.collection.JavaConverters._

abstract class BaseMonitorServerInterceptorTests extends RpcBaseTestSuite {

  import higherkindness.mu.rpc.protocol.Utils.database._
  import higherkindness.mu.rpc.prometheus.shared.RegistryHelper._

  def name: String
  def namespace: Option[String]
  def defaultInterceptorsRuntime: InterceptorsRuntime
  def allMetricsInterceptorsRuntime: InterceptorsRuntime
  def interceptorsRuntimeWithNonDefaultBuckets(buckets: Vector[Double]): InterceptorsRuntime

  s"MonitorServerInterceptor for $name" should {

    "work for unary RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(namespace)).samples.size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesReceived(namespace)).samples shouldBe empty
      findRecordedMetricOrThrow(serverMetricStreamMessagesSent(namespace)).samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow(serverMetricHandledCompleted(namespace)).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "UNARY",
          "AvroRPCService",
          "unary",
          "OK")
      }

    }

    "work for client streaming RPC metrics" in {

      ignoreOnTravis("TODO: restore once https://github.com/higherkindness/mu/issues/168 is fixed")

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(namespace)).samples.size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesReceived(namespace)).samples
        .size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesSent(namespace)).samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow(serverMetricHandledCompleted(namespace)).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "CLIENT_STREAMING",
          "ProtoRPCService",
          "clientStreaming",
          "OK")
      }
    }

    "work for server streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      val response: List[C] = (for {
        _ <- serverStart[ConcurrentMonad]
        r <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield r).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(namespace)).samples.size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesReceived(namespace)).samples shouldBe empty
      findRecordedMetricOrThrow(serverMetricStreamMessagesSent(namespace)).samples.size() shouldBe 1

      val handledSamples =
        findRecordedMetricOrThrow(serverMetricHandledCompleted(namespace)).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "ProtoRPCService",
          "serverStreaming",
          "OK"
        )
      }

      val messagesSent =
        findRecordedMetricOrThrow(serverMetricStreamMessagesSent(namespace)).samples.asScala.toList

      messagesSent.headOption.foreach { s =>
        s.value should be >= 0.doubleValue()
        s.value should be <= response.size.doubleValue()
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "ProtoRPCService",
          "serverStreaming"
        )
      }

    }

    "work for bidirectional streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(namespace)).samples.size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesReceived(namespace)).samples
        .size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesSent(namespace)).samples.size() shouldBe 1

      val handledSamples =
        findRecordedMetricOrThrow(serverMetricHandledCompleted(namespace)).samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "BIDI_STREAMING",
          "AvroRPCService",
          "biStreaming",
          "OK")
      }

    }

    "work when no histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetric(serverMetricHandledLatencySeconds(namespace)) shouldBe None

    }

    "work when histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = allMetricsInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      val metric: Option[Collector.MetricFamilySamples] =
        findRecordedMetric(serverMetricHandledLatencySeconds(namespace))

      metric shouldBe defined
      metric.map { m =>
        m.samples.size should be > 0
      }

    }

    "work for different buckets" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val buckets: Vector[Double]            = Vector[Double](0.1, 0.2, 0.8)
      val serverRuntime: InterceptorsRuntime = interceptorsRuntimeWithNonDefaultBuckets(buckets)

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      countSamples(
        serverMetricHandledLatencySeconds(namespace),
        "grpc_server_handled_latency_seconds_bucket") shouldBe (buckets.size + 1)

    }

    "work when combining multiple calls" in {

      ignoreOnTravis("TODO: restore once https://github.com/higherkindness/mu/issues/168 is fixed")

      def unary[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def clientStreaming[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      val serverRuntime: InterceptorsRuntime = defaultInterceptorsRuntime

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- unary[ConcurrentMonad]
        _ <- clientStreaming[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(namespace)).samples.size() shouldBe 2
      findRecordedMetricOrThrow(serverMetricHandledCompleted(namespace)).samples.size() shouldBe 2

    }

  }
}
