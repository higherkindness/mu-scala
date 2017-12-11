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

package freestyle.rpc
package prometheus
package server

import freestyle.rpc.common._
import freestyle.rpc.prometheus.shared.Configuration
import freestyle.rpc.withouttagless.Utils.client.MyRPCClient
import io.prometheus.client.{Collector, CollectorRegistry}

import scala.collection.JavaConverters._

class MonitorServerInterceptorTests extends RpcBaseTestSuite {

  import freestyle.rpc.server.implicits._
  import freestyle.rpc.withouttagless.Utils.database._
  import RegistryHelper._

  "MonitorServerInterceptor for Prometheus" should {

    "work for unary RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetricOrThrow("grpc_server_started_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_received_total").samples shouldBe empty
      findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow("grpc_server_handled_total").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "UNARY",
          "RPCService",
          "unary",
          "OK")
      }
      handledSamples.headOption.map { s =>
        s.value shouldBe 0.5 +- 0.5
      }

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work for client streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetricOrThrow("grpc_server_started_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_received_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow("grpc_server_handled_total").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value shouldBe 0.5 +- 0.5
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "CLIENT_STREAMING",
          "RPCService",
          "clientStreaming",
          "OK")
      }

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work for server streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      val response: List[C] = clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetricOrThrow("grpc_server_started_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_received_total").samples shouldBe empty
      findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples.size() shouldBe 1

      val handledSamples =
        findRecordedMetricOrThrow("grpc_server_handled_total").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value shouldBe 0.5 +- 0.5
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "RPCService",
          "serverStreaming",
          "OK"
        )
      }

      val messagesSent =
        findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples.asScala.toList

      messagesSent.headOption.foreach { s =>
        s.value should be >= 0.doubleValue()
        s.value should be <= response.size.doubleValue()
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "RPCService",
          "serverStreaming"
        )
      }

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work for bidirectional streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetricOrThrow("grpc_server_started_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_received_total").samples.size() shouldBe 1
      findRecordedMetricOrThrow("grpc_server_msg_sent_total").samples.size() shouldBe 1

      val handledSamples =
        findRecordedMetricOrThrow("grpc_server_handled_total").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value shouldBe 0.5 +- 0.5
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "BIDI_STREAMING",
          "RPCService",
          "biStreaming",
          "OK")
      }

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work when no histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetric("grpc_server_handled_latency_seconds") shouldBe None

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work when histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime(Configuration.defaultAllMetrics)

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val metric: Option[Collector.MetricFamilySamples] =
        findRecordedMetric("grpc_server_handled_latency_seconds")

      metric shouldBe defined
      metric.map { m =>
        m.samples.size should be > 0
      }

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work for different buckets" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val buckets: Vector[Double] = Vector[Double](0.1, 0.2, 0.8)
      val serverRuntime: InterceptorsRuntime =
        InterceptorsRuntime(Configuration.defaultAllMetrics.withLatencyBuckets(buckets))

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      clientProgram[ConcurrentMonad].unsafeRunSync()

      countSamples(
        "grpc_server_handled_latency_seconds",
        "grpc_server_handled_latency_seconds_bucket") shouldBe (buckets.size + 1)

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

    "work when combining multiple calls" in {

      def unary[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def clientStreaming[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      val serverRuntime: InterceptorsRuntime = InterceptorsRuntime()

      import serverRuntime._
      serverRuntime.serverStart[ConcurrentMonad].unsafeRunSync()
      implicit val CR: CollectorRegistry = serverRuntime.cr

      (for {
        a <- unary[ConcurrentMonad]
        b <- clientStreaming[ConcurrentMonad]
      } yield (a, b)).unsafeRunSync()

      findRecordedMetricOrThrow("grpc_server_started_total").samples.size() shouldBe 2
      findRecordedMetricOrThrow("grpc_server_handled_total").samples.size() shouldBe 2

      serverRuntime.serverStop[ConcurrentMonad].unsafeRunSync()
    }

  }
}
