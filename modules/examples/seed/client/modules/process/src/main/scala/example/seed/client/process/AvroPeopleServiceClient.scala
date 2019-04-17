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

package example.seed.client.process

import java.net.InetAddress

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import example.seed.client.common.models.PeopleError
import example.seed.client.process.runtime.handlers._
import example.seed.server.protocol.avro._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.{ManagedChannelInterpreter, UsePlaintext}
import io.chrisdavenport.log4cats.Logger
import io.grpc.{CallOptions, ManagedChannel}

trait AvroPeopleServiceClient[F[_]] {

  def getPerson(name: String): F[Either[PeopleError, Person]]

}
object AvroPeopleServiceClient {

  val serviceName = "AvroPeopleClient"

  def apply[F[_]: Effect](client: PeopleService[F])(
      implicit L: Logger[F]): AvroPeopleServiceClient[F] =
    new AvroPeopleServiceClient[F] {

      def getPerson(name: String): F[Either[PeopleError, Person]] =
        for {
          response <- client.getPerson(PeopleRequest(name))
          _ <- L.info(
            s"$serviceName - Request: $name - Result: ${response.result.map(PeopleResponseLogger).unify}")
        } yield response.result.map(PeopleResponseHandler).unify

    }

  def createClient[F[_]: ContextShift: Logger](
      hostname: String,
      port: Int)(
      implicit F: ConcurrentEffect[F]): fs2.Stream[F, AvroPeopleServiceClient[F]] = {

    val channel: F[ManagedChannel] =
      F.delay(InetAddress.getByName(hostname).getHostAddress).flatMap { ip =>
        val channelFor    = ChannelForAddress(ip, port)
        new ManagedChannelInterpreter[F](channelFor, List(UsePlaintext())).build
      }

    def clientFromChannel: Resource[F, PeopleService[F]] =
      PeopleService.clientFromChannel(channel, CallOptions.DEFAULT)

    fs2.Stream.resource(clientFromChannel).map(AvroPeopleServiceClient(_))
  }

}
