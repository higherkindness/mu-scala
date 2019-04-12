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

import cats.effect.Sync
import cats.syntax.functor._
import example.seed.server.protocol.avro._
import example.seed.server.protocol.avro.PeopleService
import io.chrisdavenport.log4cats.Logger
import shapeless.{:+:, CNil, Coproduct}

class AvroPeopleServiceHandler[F[_]: Sync](implicit L: Logger[F]) extends PeopleService[F] {

  val serviceName = "AvroPeopleService"

  type PersonResult = Person :+: NotFoundError :+: DuplicatedPersonError :+: CNil

  val people: List[Person] = List(
    Person("Foo", 10),
    Person("Bar", 20),
    Person("Bar", 10)
  )

  def getPerson(request: PeopleRequest): F[PeopleResponse] = {
    def findPerson: PersonResult =
      people.count(_.name == request.name) match {
        case x if x < 2 =>
          people
            .find(_.name == request.name)
            .map(Coproduct[PersonResult](_))
            .getOrElse(Coproduct[PersonResult](NotFoundError(s"Person ${request.name} not found")))
        case _ =>
          Coproduct[PersonResult](DuplicatedPersonError(s"Person ${request.name} duplicated"))
      }

    L.info(s"$serviceName - Request: $request").as(PeopleResponse(findPerson))
  }

}
