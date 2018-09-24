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

package freestyle.rpc.benchmarks
package shared
package server

import cats.effect._
import freestyle.rpc.benchmarks.shared.models._
import freestyle.rpc.benchmarks.shared.{PersonServiceAvro, PersonServicePB}
import freestyle.rpc.protocol.Empty
import freestyle.rpc.server._

abstract class HandlerImpl[F[_]: Effect](implicit persistenceService: PersistenceService[F]) {

  def listPersons(b: Empty.type): F[PersonList] =
    persistenceService.listPersons

  def getPerson(id: PersonId): F[Person] =
    persistenceService.getPerson(id)

  def getPersonLinks(id: PersonId): F[PersonLinkList] =
    persistenceService.getPersonLinks(id)

  def createPerson(person: Person): F[Person] =
    persistenceService.createPerson(person)
}

class RPCProtoHandler[F[_]: Effect](implicit PS: PersistenceService[F])
    extends HandlerImpl[F]
    with PersonServicePB[F]

class RPCAvroHandler[F[_]: Effect](implicit PS: PersistenceService[F])
    extends HandlerImpl[F]
    with PersonServiceAvro[F]

trait ProtoImplicits extends Runtime {

  implicit private val personServicePBHandler: RPCProtoHandler[IO] = new RPCProtoHandler[IO]

  implicit val grpcConfigsProto: List[GrpcConfig] = List(
    AddService(PersonServicePB.bindService[IO]))

}

trait AvroImplicits extends Runtime {

  implicit private val personServiceAvroHandler: RPCAvroHandler[IO] = new RPCAvroHandler[IO]

  implicit val grpcConfigsAvro: List[GrpcConfig] = List(
    AddService(PersonServiceAvro.bindService[IO]))
}
