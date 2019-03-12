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

  sealed trait GrpcStatus       extends Product with Serializable
  case object OK                extends GrpcStatus
  case object Cancelled         extends GrpcStatus
  case object DeadlineExceeded  extends GrpcStatus
  case object Internal          extends GrpcStatus
  case object ResourceExhausted extends GrpcStatus
  case object Unauthenticated   extends GrpcStatus
  case object Unavailable       extends GrpcStatus
  case object Unimplemented     extends GrpcStatus
  case object Unknown           extends GrpcStatus
  case object UnreachableError  extends GrpcStatus

  def grpcStatusFromRawStatus(status: Status): GrpcStatus = status match {
    case Status.ABORTED             => UnreachableError
    case Status.ALREADY_EXISTS      => UnreachableError
    case Status.CANCELLED           => Cancelled
    case Status.DATA_LOSS           => UnreachableError
    case Status.DEADLINE_EXCEEDED   => DeadlineExceeded
    case Status.FAILED_PRECONDITION => UnreachableError
    case Status.INTERNAL            => Internal
    case Status.INVALID_ARGUMENT    => UnreachableError
    case Status.NOT_FOUND           => UnreachableError
    case Status.OK                  => OK
    case Status.OUT_OF_RANGE        => UnreachableError
    case Status.PERMISSION_DENIED   => UnreachableError
    case Status.RESOURCE_EXHAUSTED  => ResourceExhausted
    case Status.UNAUTHENTICATED     => Unauthenticated
    case Status.UNAVAILABLE         => Unavailable
    case Status.UNIMPLEMENTED       => Unimplemented
    case Status.UNKNOWN             => Unknown
    case _                          => UnreachableError
  }

  def statusDescription(status: GrpcStatus): String = status match {
    case OK                => "ok-statuses"
    case Cancelled         => "cancelled-statuses"
    case DeadlineExceeded  => "deadline-exceeded-statuses"
    case Internal          => "internal-statuses"
    case ResourceExhausted => "resource-exhausted-statuses"
    case Unauthenticated   => "unauthenticated-statuses"
    case Unavailable       => "unavailable-statuses"
    case Unimplemented     => "unimplemented-statuses"
    case Unknown           => "unknown-statuses"
    case _                 => "unreachable-error-statuses"
  }

  def methodTypeDescription(methodInfo: GrpcMethodInfo): String =
    methodInfo.`type` match {
      case UNARY            => "unary-methods"
      case CLIENT_STREAMING => "client-streaming-methods"
      case SERVER_STREAMING => "server-streaming-methods"
      case BIDI_STREAMING   => "bidi-streaming-methods"
      case UNKNOWN          => "unknown-methods"
    }

}
