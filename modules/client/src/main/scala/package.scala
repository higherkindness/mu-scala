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

import cats.data.Kleisli

import scala.collection.JavaConverters._
import io.grpc._

package object client {

  type ManagedChannelOps[F[_], A] = Kleisli[F, F[ManagedChannel], A]

  def configureChannel[T <: ManagedChannelBuilder[T]](mcb: T, conf: ManagedChannelConfig): T =
    conf match {
      case DirectExecutor                    => mcb.directExecutor()
      case SetExecutor(executor)             => mcb.executor(executor)
      case AddInterceptorList(interceptors)  => mcb.intercept(interceptors.asJava)
      case AddInterceptor(interceptors @ _*) => mcb.intercept(interceptors: _*)
      case UserAgent(userAgent)              => mcb.userAgent(userAgent)
      case OverrideAuthority(authority)      => mcb.overrideAuthority(authority)
      case UsePlaintext()                    => mcb.usePlaintext()
      case NameResolverFactory(rf)           => mcb.nameResolverFactory(rf)
      case LoadBalancerFactory(lbf)          => mcb.loadBalancerFactory(lbf)
      case SetDecompressorRegistry(registry) => mcb.decompressorRegistry(registry)
      case SetCompressorRegistry(registry)   => mcb.compressorRegistry(registry)
      case SetIdleTimeout(value, unit)       => mcb.idleTimeout(value, unit)
      case SetMaxInboundMessageSize(max)     => mcb.maxInboundMessageSize(max)
    }
}
