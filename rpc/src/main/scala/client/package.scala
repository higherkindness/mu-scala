/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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
import freestyle.free.FreeS

import scala.collection.JavaConverters._
import io.grpc._

package object client {

  type ManagedChannelOps[F[_], A] = Kleisli[F, ManagedChannel, A]

  class ManagedChannelInterpreter[F[_]](
      initConfig: ManagedChannelFor,
      configList: List[ManagedChannelConfig])
      extends (Kleisli[F, ManagedChannel, ?] ~> F) {

    def build(
        initConfig: ManagedChannelFor,
        configList: List[ManagedChannelConfig]): ManagedChannel = {
      val builder: ManagedChannelBuilder[_] = initConfig match {
        case ManagedChannelForAddress(name, port) => ManagedChannelBuilder.forAddress(name, port)
        case ManagedChannelForTarget(target)      => ManagedChannelBuilder.forTarget(target)
      }

      configList
        .foldLeft(builder) { (acc, cfg) =>
          cfg match {
            case DirectExecutor                    => acc.directExecutor()
            case SetExecutor(executor)             => acc.executor(executor)
            case AddInterceptorList(interceptors)  => acc.intercept(interceptors.asJava)
            case AddInterceptor(interceptors @ _*) => acc.intercept(interceptors: _*)
            case UserAgent(userAgent)              => acc.userAgent(userAgent)
            case OverrideAuthority(authority)      => acc.overrideAuthority(authority)
            case UsePlaintext(skipNegotiation)     => acc.usePlaintext(skipNegotiation)
            case NameResolverFactory(rf)           => acc.nameResolverFactory(rf)
            case LoadBalancerFactory(lbf)          => acc.loadBalancerFactory(lbf)
            case SetDecompressorRegistry(registry) => acc.decompressorRegistry(registry)
            case SetCompressorRegistry(registry)   => acc.compressorRegistry(registry)
            case SetIdleTimeout(value, unit)       => acc.idleTimeout(value, unit)
            case SetMaxInboundMessageSize(max)     => acc.maxInboundMessageSize(max)
          }
        }
        .build()
    }

    override def apply[A](fa: Kleisli[F, ManagedChannel, A]): F[A] =
      fa(build(initConfig, configList))
  }

  def ConfigForAddress[F[_]](hostPath: String, portPath: String)(
      implicit CC: ChannelConfig[F]): FreeS[F, ManagedChannelForAddress] =
    CC.loadChannelAddress(hostPath, portPath)

  def ConfigForTarget[F[_]](target: String)(
      implicit CC: ChannelConfig[F]): FreeS[F, ManagedChannelForTarget] =
    CC.loadChannelTarget(target)
}
