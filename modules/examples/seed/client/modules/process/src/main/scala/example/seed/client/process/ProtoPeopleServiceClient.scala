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
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import example.seed.server.protocol.proto.people._
import example.seed.server.protocol.proto.services._
import fs2._
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.{ManagedChannelInterpreter, UsePlaintext}
import io.chrisdavenport.log4cats.Logger
import io.grpc.{CallOptions, ManagedChannel}

import scala.util.Random
import scala.concurrent.duration._

trait ProtoPeopleServiceClient[F[_]] {

  def getPerson(name: String): F[Person]

  def getRandomPersonStream: Stream[F, Person]

}
object ProtoPeopleServiceClient {

  def apply[F[_]](client: PeopleService[F])(
      implicit F: Effect[F],
      L: Logger[F],
      T: Timer[F]): ProtoPeopleServiceClient[F] =
    new ProtoPeopleServiceClient[F] {

      val serviceName = "ProtoPeopleClient"

      def getPerson(name: String): F[Person] =
        for {
          _      <- L.info(s"")
          result <- client.getPerson(PeopleRequest(name))
          _      <- L.info(s"$serviceName - Request: $name - Result: $result")
        } yield result.person

      def getRandomPersonStream: Stream[F, Person] = {

        def requestStream: Stream[F, PeopleRequest] =
          Stream.iterateEval(PeopleRequest("")) { _ =>
            val req = PeopleRequest(Random.nextPrintableChar().toString)
            T.sleep(2.seconds) *> L.info(s"$serviceName Stream Request: $req").as(req)
          }

        for {
          result <- client.getPersonStream(requestStream)
          _      <- Stream.eval(L.info(s"$serviceName Stream Result: $result"))
        } yield result.person
      }

    }

  def createClient[F[_]: ContextShift: Logger: Timer](
      hostname: String,
      port: Int)(
      implicit F: ConcurrentEffect[F]): fs2.Stream[F, ProtoPeopleServiceClient[F]] = {

    val channel: F[ManagedChannel] =
      F.delay(InetAddress.getByName(hostname).getHostAddress).flatMap { ip =>
        val channelFor    = ChannelForAddress(ip, port)
        new ManagedChannelInterpreter[F](channelFor, List(UsePlaintext())).build
      }

    def clientFromChannel: Resource[F, PeopleService[F]] =
      PeopleService.clientFromChannel(channel, CallOptions.DEFAULT)

    fs2.Stream.resource(clientFromChannel).map(ProtoPeopleServiceClient(_))
  }

}
