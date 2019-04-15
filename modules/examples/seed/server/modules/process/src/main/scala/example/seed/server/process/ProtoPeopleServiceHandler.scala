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

package example.seed.server.process

import cats.effect.{Sync, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import example.seed.server.protocol.proto.people._
import example.seed.server.protocol.proto.services.PeopleService
import fs2._
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration._

class ProtoPeopleServiceHandler[F[_]: Timer](implicit F: Sync[F], L: Logger[F])
    extends PeopleService[F] {

  val serviceName = "ProtoPeopleService"

  def getPerson(request: PeopleRequest): F[PeopleResponse] =
    L.info(s"$serviceName - Request: $request").as(PeopleResponse(Person(request.name, 10)))

  def getPersonStream(request: Stream[F, PeopleRequest]): Stream[F, PeopleResponse] = {

    def responseStream(person: PeopleRequest): Stream[F, PeopleResponse] = {
      val response = PeopleResponse(Person(person.name, 10))
      Stream
        .awakeEvery[F](2.seconds)
        .evalMap(
          _ =>
            L.info(s"$serviceName - Stream Response: $response")
              .as(response))
    }

    for {
      person   <- request
      _        <- Stream.eval(L.info(s"$serviceName - Stream Request: $person"))
      response <- responseStream(person)
    } yield response
  }

}
