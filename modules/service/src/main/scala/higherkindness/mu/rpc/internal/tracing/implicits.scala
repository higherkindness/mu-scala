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
