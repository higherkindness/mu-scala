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

package higherkindness.mu.rpc.internal

import io.grpc.{Status, StatusException, StatusRuntimeException}
import io.grpc.stub.{ServerCallStreamObserver, StreamObserver}

package object server {

  private[internal] def addCompression[A](
      observer: StreamObserver[A],
      algorithm: Option[String]): Unit =
    (observer, algorithm) match {
      case (o: ServerCallStreamObserver[_], Some(alg)) => o.setCompression(alg)
      case _                                           =>
    }

  private[internal] def completeObserver[A](
      observer: StreamObserver[A]): Either[Throwable, A] => Unit = {
    case Right(value) =>
      observer.onNext(value)
      observer.onCompleted()
    case Left(s: StatusException) =>
      observer.onError(s)
    case Left(s: StatusRuntimeException) =>
      observer.onError(s)
    case Left(e) =>
      observer.onError(
        Status.INTERNAL.withDescription(e.getMessage).withCause(e).asException()
      )
  }

}
