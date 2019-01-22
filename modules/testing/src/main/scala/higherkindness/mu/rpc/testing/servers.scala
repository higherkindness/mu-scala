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
package testing

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.syntax.apply._
import cats.syntax.functor._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.internal.NoopServerCall.NoopServerCallListener
import io.grpc.util.MutableHandlerRegistry
import io.grpc._

object servers {

  def serverCallHandler[Req, Res]: ServerCallHandler[Req, Res] = new ServerCallHandler[Req, Res] {
    override def startCall(
        call: ServerCall[Req, Res],
        headers: Metadata): ServerCall.Listener[Req] = new NoopServerCallListener[Req]
  }

  def serverServiceDefinition(
      serviceName: String,
      methodList: List[String]): ServerServiceDefinition = {
    val ssdBuilder = ServerServiceDefinition.builder(serviceName)
    methodList.foreach { methodName =>
      ssdBuilder.addMethod(methods.voidMethod(Some(methodName)), serverCallHandler[Void, Void])
    }
    ssdBuilder.build()
  }

  def withServerChannel[F[_]: Sync](
      service: F[ServerServiceDefinition],
      clientInterceptor: Option[ClientInterceptor] = None): Resource[F, ServerChannel] =
    withServerChannelList(service.map(List(_)), clientInterceptor)

  def withServerChannelList[F[_]: Sync](
      services: F[List[ServerServiceDefinition]],
      clientInterceptor: Option[ClientInterceptor] = None): Resource[F, ServerChannel] =
    Resource.liftF(services).flatMap(ServerChannel.fromList(_, clientInterceptor))

  final case class ServerChannel(server: Server, channel: ManagedChannel)

  object ServerChannel {

    def apply[F[_]: Sync](
        serverServiceDefinition: ServerServiceDefinition,
        clientInterceptor: Option[ClientInterceptor] = None): Resource[F, ServerChannel] =
      ServerChannel.fromList[F](List(serverServiceDefinition), clientInterceptor)

    def fromList[F[_]: Sync](
        serverServiceDefinitions: List[ServerServiceDefinition],
        clientInterceptor: Option[ClientInterceptor] = None): Resource[F, ServerChannel] = {
      val serviceRegistry =
        new MutableHandlerRegistry
      val serverName: String =
        UUID.randomUUID.toString
      val serverBuilder: InProcessServerBuilder =
        InProcessServerBuilder
          .forName(serverName)
          .fallbackHandlerRegistry(serviceRegistry)
          .directExecutor()
      val channelBuilder: InProcessChannelBuilder =
        InProcessChannelBuilder.forName(serverName)

      clientInterceptor.foreach { interceptor =>
        channelBuilder.intercept(interceptor)
      }

      serverServiceDefinitions.map(serverBuilder.addService)

      Resource.make {
        (
          Sync[F].delay(serverBuilder.build().start()),
          Sync[F].delay(channelBuilder.directExecutor.build)).mapN(ServerChannel(_, _))
      }(sc => Sync[F].delay(sc.server.shutdown()).void)
    }

  }

}
