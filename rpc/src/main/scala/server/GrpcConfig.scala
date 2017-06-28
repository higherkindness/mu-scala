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
package server

import java.io.File
import java.util.concurrent.Executor

import io.grpc._

case class Config(port: Int)

sealed trait GrpcConfig                                                  extends Product with Serializable
case object DirectExecutor                                               extends GrpcConfig
case class SetExecutor(executor: Executor)                               extends GrpcConfig
case class AddService(service: ServerServiceDefinition)                  extends GrpcConfig
case class AddBindableService(bindableService: BindableService)          extends GrpcConfig
case class AddTransportFilter(filter: ServerTransportFilter)             extends GrpcConfig
case class AddStreamTracerFactory(factory: ServerStreamTracer.Factory)   extends GrpcConfig
case class SetFallbackHandlerRegistry(fallbackRegistry: HandlerRegistry) extends GrpcConfig
case class UseTransportSecurity(certChain: File, privateKey: File)       extends GrpcConfig
case class SetDecompressorRegistry(registry: DecompressorRegistry)       extends GrpcConfig
case class SetCompressorRegistry(registry: CompressorRegistry)           extends GrpcConfig
