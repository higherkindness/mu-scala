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

package freestyle.rpc

import cats.data.Kleisli
import cats.~>

import scala.collection.JavaConverters._
import io.grpc._

package object client {

  type ManagedChannelOps[F[_], A] = Kleisli[F, ManagedChannel, A]

  class ManagedChannelInterpreter[F[_]](
      initConfig: ChannelFor,
      configList: List[ManagedChannelConfig])
      extends (Kleisli[F, ManagedChannel, ?] ~> F) {

    def build[T <: ManagedChannelBuilder[T]](
        initConfig: ChannelFor,
        configList: List[ManagedChannelConfig]): ManagedChannel = {
      val builder: T = initConfig match {
        case ChannelForAddress(name, port) =>
          ManagedChannelBuilder.forAddress(name, port).asInstanceOf[T]
        case ChannelForTarget(target) => ManagedChannelBuilder.forTarget(target).asInstanceOf[T]
        case e =>
          throw new IllegalArgumentException(s"ManagedChannel not supported for $e")
      }

      configList
        .foldLeft(builder) { (acc, cfg) =>
          ManagedChannelB(acc)(cfg)
        }
        .build()
    }

    override def apply[A](fa: Kleisli[F, ManagedChannel, A]): F[A] =
      fa(build(initConfig, configList))
  }

  def ManagedChannelB[T <: ManagedChannelBuilder[T]](
      mcb: T): PartialFunction[ManagedChannelConfig, T] = {
    case DirectExecutor                    => mcb.directExecutor()
    case SetExecutor(executor)             => mcb.executor(executor)
    case AddInterceptorList(interceptors)  => mcb.intercept(interceptors.asJava)
    case AddInterceptor(interceptors @ _*) => mcb.intercept(interceptors: _*)
    case UserAgent(userAgent)              => mcb.userAgent(userAgent)
    case OverrideAuthority(authority)      => mcb.overrideAuthority(authority)
    case UsePlaintext(skipNegotiation)     => mcb.usePlaintext(skipNegotiation)
    case NameResolverFactory(rf)           => mcb.nameResolverFactory(rf)
    case LoadBalancerFactory(lbf)          => mcb.loadBalancerFactory(lbf)
    case SetDecompressorRegistry(registry) => mcb.decompressorRegistry(registry)
    case SetCompressorRegistry(registry)   => mcb.compressorRegistry(registry)
    case SetIdleTimeout(value, unit)       => mcb.idleTimeout(value, unit)
    case SetMaxInboundMessageSize(max)     => mcb.maxInboundMessageSize(max)
  }
}
