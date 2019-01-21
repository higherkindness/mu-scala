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
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status
import org.scalacheck.Gen.alphaLowerChar
import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop.forAll

import scala.collection.JavaConverters._

object DropWizardMetricsOpsTests extends Properties("DropWizardMetrics") {

  val prefix     = "testPrefix"
  val classifier = Some("classifier")

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

  def checkMetrics(
      methodInfo: GrpcMethodInfo,
      numberOfCalls: Int,
      expectedCount: Int,
      registry: MetricRegistry,
      eventName: String,
      getGauges: MetricRegistry => List[String],
      getGaugeCount: (MetricRegistry, String) => Long,
      f: => IO[Unit],
      status: Option[Status] = None): Boolean = {

    (1 to numberOfCalls).toList
      .map { _ =>
        f
      }
      .sequence
      .unsafeRunSync()

    val counters    = getGauges(registry)
    val counterName = eventDescription(prefix, classifier, methodInfo, eventName, status)

    counters.contains(counterName) && getGaugeCount(registry, counterName) == expectedCount
  }

  property("creates and updates counter when registering an active call") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        checkMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          "active-calls",
          { _.getCounters().asScala.keys.toList }, { (registry, name) =>
            registry.counter(name).getCount
          },
          { metrics.increaseActiveCalls(methodInfo, classifier) }
        ) && checkMetrics(
          methodInfo,
          numberOfCalls,
          0,
          registry,
          "active-calls",
          { _.getCounters().asScala.keys.toList }, { (registry, name) =>
            registry.counter(name).getCount
          },
          { metrics.decreaseActiveCalls(methodInfo, classifier) }
        )
    }

  property("creates and updates counter when registering a sent message") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        checkMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          "message-sent",
          { _.getCounters().asScala.keys.toList }, { (registry, name) =>
            registry.counter(name).getCount
          },
          { metrics.recordMessageSent(methodInfo, classifier) }
        )
    }

  property("creates and updates counter when registering a received message") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        checkMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          "message-received",
          { _.getCounters().asScala.keys.toList }, { (registry, name) =>
            registry.counter(name).getCount
          },
          { metrics.recordMessageReceived(methodInfo, classifier) }
        )
    }

  property("creates and updates timer for headers time") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10), Gen.chooseNum(100, 1000)) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, elapsed: Int) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        checkMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          "headers-time", { _.getTimers().asScala.keys.toList }, { (registry, name) =>
            registry.timer(name).getCount
          },
          { metrics.recordHeadersTime(methodInfo, elapsed.toLong, classifier) }
        )
    }

  property("creates and updates timer for total time") =
    forAll(methodInfoGen, Gen.chooseNum[Int](1, 10), Gen.chooseNum(100, 1000), statusGen) {
      (methodInfo: GrpcMethodInfo, numberOfCalls: Int, elapsed: Int, status: Status) =>
        val registry = new MetricRegistry()
        val metrics  = DropWizardMetricsOps[IO](registry, prefix)

        checkMetrics(
          methodInfo,
          numberOfCalls,
          numberOfCalls,
          registry,
          "total-time", { _.getTimers().asScala.keys.toList }, { (registry, name) =>
            registry.timer(name).getCount
          },
          { metrics.recordTotalTime(methodInfo, status, elapsed.toLong, classifier) },
          Some(status)
        )
    }

}
