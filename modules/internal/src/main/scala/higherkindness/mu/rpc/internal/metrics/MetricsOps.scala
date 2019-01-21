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

package higherkindness.mu.rpc.internal.metrics
import higherkindness.mu.rpc.internal.interceptors.GrpcMethodInfo
import io.grpc.MethodDescriptor.MethodType._
import io.grpc.Status

trait MetricsOps[F[_]] {

  def increaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit]

  def decreaseActiveCalls(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit]

  def recordMessageSent(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit]

  def recordMessageReceived(methodInfo: GrpcMethodInfo, classifier: Option[String]): F[Unit]

  def recordHeadersTime(
      methodInfo: GrpcMethodInfo,
      elapsed: Long,
      classifier: Option[String]): F[Unit]

  def recordTotalTime(
      methodInfo: GrpcMethodInfo,
      status: Status,
      elapsed: Long,
      classifier: Option[String]): F[Unit]

}

object MetricsOps {

  def eventDescription(
      prefix: String,
      classifier: Option[String],
      methodInfo: GrpcMethodInfo,
      eventName: String,
      status: Option[Status] = None) =
    classifier
      .map(c => s"$prefix.$c")
      .getOrElse(s"$prefix.default") + "." + methodInfoDescription(methodInfo) + s".$eventName" + status
      .map(s => s".${statusDescription(s)}")
      .getOrElse("")

  private def methodInfoDescription(methodInfo: GrpcMethodInfo): String =
    methodInfo.`type` match {
      case UNARY => s"${methodInfo.serviceName}.${methodInfo.methodName}.unary"
      case CLIENT_STREAMING =>
        s"${methodInfo.serviceName}.${methodInfo.methodName}.client-streaming"
      case SERVER_STREAMING =>
        s"${methodInfo.serviceName}.${methodInfo.methodName}.server-streaming"
      case BIDI_STREAMING => s"${methodInfo.serviceName}.${methodInfo.methodName}.bidi-streaming"
      case UNKNOWN        => s"${methodInfo.serviceName}.${methodInfo.methodName}.unknown"
    }

  private def statusDescription(status: Status): String = status match {
    case Status.ABORTED             => "aborted"
    case Status.ALREADY_EXISTS      => "already-exists"
    case Status.CANCELLED           => "cancelled"
    case Status.DATA_LOSS           => "data-loss"
    case Status.DEADLINE_EXCEEDED   => "deadline-exceeded"
    case Status.FAILED_PRECONDITION => "failed-precondition"
    case Status.INTERNAL            => "internal"
    case Status.INVALID_ARGUMENT    => "invalid-argument"
    case Status.NOT_FOUND           => "not-found"
    case Status.OK                  => "ok"
    case Status.OUT_OF_RANGE        => "out-of-range"
    case Status.PERMISSION_DENIED   => "permission-denied"
    case Status.RESOURCE_EXHAUSTED  => "resource-exhausted"
    case Status.UNAUTHENTICATED     => "unauthenticated"
    case Status.UNAVAILABLE         => "unavailable"
    case Status.UNIMPLEMENTED       => "unimplemented"
    case Status.UNKNOWN             => "unknown"
  }

}
