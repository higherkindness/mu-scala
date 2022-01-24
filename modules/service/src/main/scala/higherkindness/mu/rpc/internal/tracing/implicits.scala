/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.internal.tracing

import cats.effect.{Async, Resource}
import cats.syntax.all._
import higherkindness.mu.rpc.internal.context.{ClientContext, ClientContextMetaData, ServerContext}
import io.grpc.Metadata.{ASCII_STRING_MARSHALLER, BINARY_HEADER_SUFFIX, Key}
import io.grpc.{CallOptions, Channel, Metadata, MethodDescriptor}
import natchez.{EntryPoint, Kernel, Span}

import scala.jdk.CollectionConverters._

object implicits {

  implicit def clientContext[F[_]: Async]: ClientContext[F, Span[F]] =
    new ClientContext[F, Span[F]] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          channel: Channel,
          options: CallOptions,
          current: Span[F]
      ): Resource[F, ClientContextMetaData[Span[F]]] =
        current
          .span(descriptor.getFullMethodName)
          .evalMap { span =>
            span.kernel.map(tracingKernelToHeaders).map(ClientContextMetaData(span, _))
          }
    }

  implicit def serverContext[F[_]](implicit entrypoint: EntryPoint[F]): ServerContext[F, Span[F]] =
    new ServerContext[F, Span[F]] {
      override def apply[Req, Res](
          descriptor: MethodDescriptor[Req, Res],
          metadata: Metadata
      ): Resource[F, Span[F]] =
        entrypoint.continueOrElseRoot(descriptor.getFullMethodName, extractTracingKernel(metadata))

    }

  def tracingKernelToHeaders(kernel: Kernel): Metadata = {
    val headers = new Metadata()
    kernel.toHeaders.foreach { case (k, v) =>
      headers.put(Key.of(k, ASCII_STRING_MARSHALLER), v)
    }
    headers
  }

  def extractTracingKernel(headers: Metadata): Kernel = {
    val asciiHeaders = headers
      .keys()
      .asScala
      .collect {
        case k if !k.endsWith(BINARY_HEADER_SUFFIX) =>
          k -> headers.get(Key.of(k, ASCII_STRING_MARSHALLER))
      }
      .toMap
    Kernel(asciiHeaders)
  }

}
