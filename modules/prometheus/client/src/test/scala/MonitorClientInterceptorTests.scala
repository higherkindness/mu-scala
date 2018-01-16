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
package client

import freestyle.rpc.common._
import freestyle.rpc.prometheus.shared.Configuration
import freestyle.rpc.withouttagless.Utils.client.MyRPCClient
import io.prometheus.client.{Collector, CollectorRegistry}
import org.scalatest.BeforeAndAfterAll

import scala.collection.JavaConverters._

class MonitorClientInterceptorTests extends RpcBaseTestSuite with BeforeAndAfterAll {

  import freestyle.rpc.withouttagless.Utils.database._
  import freestyle.rpc.prometheus.shared.RegistryHelper._
  import freestyle.rpc.prometheus.client.implicits._

  override protected def beforeAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStart[ConcurrentMonad].unsafeRunSync()
  }

  override protected def afterAll(): Unit = {
    import freestyle.rpc.server.implicits._
    serverStop[ConcurrentMonad].unsafeRunSync()
  }

  "MonitorServerInterceptor for Prometheus" should {

    "work for unary RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double = extractMetricValue("grpc_client_started_total")
      startedTotal should be >= 0d
      startedTotal should be <= 1d
      findRecordedMetricOrThrow("grpc_client_msg_received_total").samples shouldBe empty
      findRecordedMetricOrThrow("grpc_client_msg_sent_total").samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow("grpc_client_completed").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "UNARY",
          "RPCService",
          "unary",
          "OK")
      }

    }

    "work for client streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      def clientProgram2[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(List(c1), i)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double = extractMetricValue("grpc_client_started_total")

      startedTotal should be >= 0d
      startedTotal should be <= 1d

      val msgSentTotal: Double = extractMetricValue("grpc_client_msg_sent_total")
      msgSentTotal should be >= 0d
      msgSentTotal should be <= 2d

      findRecordedMetricOrThrow("grpc_client_msg_received_total").samples shouldBe empty

      clientProgram2[ConcurrentMonad].unsafeRunSync()

      val msgSentTotal2: Double = extractMetricValue("grpc_client_msg_sent_total")
      msgSentTotal2 should be >= 0d
      msgSentTotal2 should be <= 3d

      val handledSamples =
        findRecordedMetricOrThrow("grpc_client_completed").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 2d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "CLIENT_STREAMING",
          "RPCService",
          "clientStreaming",
          "OK")
      }

    }

    "work for server streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[List[C]] =
        APP.ss(a2.x, a2.y)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      val response: List[C] = clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double     = extractMetricValue("grpc_client_started_total")
      val msgReceivedTotal: Double = extractMetricValue("grpc_client_msg_received_total")
      findRecordedMetricOrThrow("grpc_client_msg_sent_total").samples shouldBe empty

      startedTotal should be >= 0d
      startedTotal should be <= 1d
      msgReceivedTotal should be >= 0d
      msgReceivedTotal should be <= 2d

      val handledSamples =
        findRecordedMetricOrThrow("grpc_client_completed").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d
        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "SERVER_STREAMING",
          "RPCService",
          "serverStreaming",
          "OK"
        )
      }

    }

    "work for bidirectional streaming RPC metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[E] =
        APP.bs(eList)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val startedTotal: Double     = extractMetricValue("grpc_client_started_total")
      val msgReceivedTotal: Double = extractMetricValue("grpc_client_msg_received_total")
      val msgSentTotal: Double     = extractMetricValue("grpc_client_msg_sent_total")

      startedTotal should be >= 0d
      startedTotal should be <= 1d
      msgReceivedTotal should be >= 0d
      msgReceivedTotal should be <= 4d
      msgSentTotal should be >= 0d
      msgSentTotal should be <= 2d

      val handledSamples =
        findRecordedMetricOrThrow("grpc_client_completed").samples.asScala.toList
      handledSamples.size shouldBe 1
      handledSamples.headOption.foreach { s =>
        s.value should be >= 0d
        s.value should be <= 1d

        s.labelValues.asScala.toList should contain theSameElementsAs Vector(
          "BIDI_STREAMING",
          "RPCService",
          "biStreaming",
          "OK")
      }

    }

    "work when no histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      findRecordedMetric("grpc_client_completed_latency_seconds") shouldBe None

    }

    "work when histogram is enabled" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultAllMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      val metric: Option[Collector.MetricFamilySamples] =
        findRecordedMetric("grpc_client_completed_latency_seconds")

      metric shouldBe defined
      metric.map { m =>
        m.samples.size should be > 0
      }

    }

    "work for different buckets" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val buckets: Vector[Double]        = Vector[Double](0.1, 0.2)
      val configuration: Configuration =
        Configuration.defaultAllMetrics.withCollectorRegistry(CR).withLatencyBuckets(buckets)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      clientProgram[ConcurrentMonad].unsafeRunSync()

      countSamples(
        "grpc_client_completed_latency_seconds",
        "grpc_client_completed_latency_seconds_bucket") shouldBe (buckets.size + 1)

    }

    "work when combining multiple calls" in {

      def unary[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      def clientStreaming[F[_]](implicit APP: MyRPCClient[F]): F[D] =
        APP.cs(cList, i)

      implicit val CR: CollectorRegistry = new CollectorRegistry()
      val configuration: Configuration   = Configuration.defaultBasicMetrics.withCollectorRegistry(CR)

      val clientRuntime: ClientRuntime = ClientRuntime(configuration)

      import clientRuntime._

      (for {
        a <- unary[ConcurrentMonad]
        b <- clientStreaming[ConcurrentMonad]
      } yield (a, b)).unsafeRunSync()

      findRecordedMetricOrThrow("grpc_client_started_total").samples.size() shouldBe 2
      findRecordedMetricOrThrow("grpc_client_completed").samples.size() shouldBe 2

    }

  }
}
