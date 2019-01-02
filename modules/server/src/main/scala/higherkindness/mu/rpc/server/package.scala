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

package higherkindness.mu.rpc

import cats.data.Kleisli
import higherkindness.mu.rpc.server.netty._
import io.grpc.{ServerBuilder, _}
import io.grpc.netty.NettyServerBuilder

package object server {

  private[server] type GrpcServerOps[F[_], A] = Kleisli[F, Server, A]

  val defaultPort = 50051

  private[server] def SBuilder[SB <: ServerBuilder[SB]](
      sb: ServerBuilder[SB]): PartialFunction[GrpcConfig, SB] = {
    case DirectExecutor                  => sb.directExecutor()
    case SetExecutor(ex)                 => sb.executor(ex)
    case AddService(srv)                 => sb.addService(srv)
    case AddBindableService(srv)         => sb.addService(srv)
    case AddTransportFilter(filter)      => sb.addTransportFilter(filter)
    case AddStreamTracerFactory(factory) => sb.addStreamTracerFactory(factory)
    case SetFallbackHandlerRegistry(fr)  => sb.fallbackHandlerRegistry(fr)
    case UseTransportSecurity(cc, pk)    => sb.useTransportSecurity(cc, pk)
    case SetDecompressorRegistry(dr)     => sb.decompressorRegistry(dr)
    case SetCompressorRegistry(cr)       => sb.compressorRegistry(cr)
  }

  private[server] def NettySBuilder(
      nsb: NettyServerBuilder): PartialFunction[GrpcConfig, NettyServerBuilder] = {
    case ChannelType(channelType)             => nsb.channelType(channelType)
    case WithChildOption(option, value)       => nsb.withChildOption(option, value)
    case BossEventLoopGroup(group)            => nsb.bossEventLoopGroup(group)
    case WorkerEventLoopGroup(group)          => nsb.workerEventLoopGroup(group)
    case SetSslContext(sslContext)            => nsb.sslContext(sslContext)
    case SetProtocolNegotiator(pn)            => nsb.protocolNegotiator(pn)
    case MaxConcurrentCallsPerConnection(mc)  => nsb.maxConcurrentCallsPerConnection(mc)
    case FlowControlWindow(flowControlWindow) => nsb.flowControlWindow(flowControlWindow)
    case MaxMessageSize(maxMessageSize)       => nsb.maxInboundMessageSize(maxMessageSize)
    case MaxHeaderListSize(maxHeaderListSize) => nsb.maxInboundMetadataSize(maxHeaderListSize)
    case KeepAliveTime(kat, timeUnit)         => nsb.keepAliveTime(kat, timeUnit)
    case KeepAliveTimeout(kato, timeUnit)     => nsb.keepAliveTimeout(kato, timeUnit)
    case MaxConnectionIdle(mci, tu)           => nsb.maxConnectionIdle(mci, tu)
    case MaxConnectionAge(mca, tu)            => nsb.maxConnectionAge(mca, tu)
    case MaxConnectionAgeGrace(mcag, tu)      => nsb.maxConnectionAgeGrace(mcag, tu)
    case PermitKeepAliveTime(kat, tu)         => nsb.permitKeepAliveTime(kat, tu)
    case PermitKeepAliveWithoutCalls(permit)  => nsb.permitKeepAliveWithoutCalls(permit)
  }
}
