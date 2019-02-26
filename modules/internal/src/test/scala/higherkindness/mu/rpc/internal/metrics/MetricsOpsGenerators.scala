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
import io.grpc.MethodDescriptor.MethodType
import io.grpc.Status
import org.scalacheck.Gen
import org.scalacheck.Gen.alphaLowerChar

object MetricsOpsGenerators {

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

}
