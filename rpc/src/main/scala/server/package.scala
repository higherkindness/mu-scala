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
import io.grpc._

package object server {

  type GrpcServerOps[F[_], A] = Kleisli[F, Server, A]

  class GrpcConfigInterpreter[F[_]](implicit initConfig: Config, configList: List[GrpcConfig])
      extends (Kleisli[F, Server, ?] ~> F) {

    private[this] def build(configList: List[GrpcConfig]): Server =
      configList
        .foldLeft(ServerBuilder.forPort(initConfig.port))((acc, option) =>
          option match {
            case DirectExecutor                  => acc.directExecutor()
            case SetExecutor(ex)                 => acc.executor(ex)
            case AddService(srv)                 => acc.addService(srv)
            case AddBindableService(srv)         => acc.addService(srv)
            case AddTransportFilter(filter)      => acc.addTransportFilter(filter)
            case AddStreamTracerFactory(factory) => acc.addStreamTracerFactory(factory)
            case SetFallbackHandlerRegistry(fr)  => acc.fallbackHandlerRegistry(fr)
            case UseTransportSecurity(cc, pk)    => acc.useTransportSecurity(cc, pk)
            case SetDecompressorRegistry(dr)     => acc.decompressorRegistry(dr)
            case SetCompressorRegistry(cr)       => acc.compressorRegistry(cr)
        })
        .build()

    override def apply[B](fa: Kleisli[F, Server, B]): F[B] =
      fa(build(configList))

  }
}
