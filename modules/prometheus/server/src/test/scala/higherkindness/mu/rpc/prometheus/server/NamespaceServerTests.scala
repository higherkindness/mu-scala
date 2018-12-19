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
import higherkindness.mu.rpc.interceptors.metrics._
import higherkindness.mu.rpc.prometheus.shared.Configuration
import higherkindness.mu.rpc.protocol.Utils.client.MyRPCClient
import org.scalatest.OptionValues

import scala.collection.JavaConverters._

class NamespaceServerTests extends RpcBaseTestSuite with OptionValues {

  import higherkindness.mu.rpc.prometheus.shared.RegistryHelper._
  import higherkindness.mu.rpc.protocol.Utils.database._

  val namespace: String                   = "custom_nsp"
  lazy val maybeNamespace: Option[String] = Some(namespace)

  s"Server Metrics allows custom namespaces" should {

    "work for Server Metrics" in {

      def clientProgram[F[_]](implicit APP: MyRPCClient[F]): F[C] =
        APP.u(a1.x, a1.y)

      val serverRuntime: InterceptorsRuntime =
        InterceptorsRuntime(Configuration.defaultBasicMetrics.withNamespace(namespace))

      import serverRuntime._

      (for {
        _ <- serverStart[ConcurrentMonad]
        _ <- clientProgram[ConcurrentMonad]
        _ <- serverStop[ConcurrentMonad]
      } yield (): Unit).unsafeRunSync()

      findRecordedMetricOrThrow(serverMetricRpcStarted(maybeNamespace)).samples.size() shouldBe 1
      findRecordedMetricOrThrow(serverMetricStreamMessagesReceived(maybeNamespace)).samples shouldBe empty
      findRecordedMetricOrThrow(serverMetricStreamMessagesSent(maybeNamespace)).samples shouldBe empty

      val handledSamples =
        findRecordedMetricOrThrow(serverMetricHandledCompleted(maybeNamespace)).samples.asScala.toList
      handledSamples.headOption.map(_.name shouldBe s"${namespace}_server_handled_total")
      handledSamples.headOption should not be 'empty
    }
  }
}
