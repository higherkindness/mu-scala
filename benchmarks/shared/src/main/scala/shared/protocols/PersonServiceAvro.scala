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

package higherkindness.mu.rpc.benchmarks
package shared
package protocols

import higherkindness.mu.rpc.benchmarks.shared.models._
import higherkindness.mu.rpc.protocol._

@service(Avro)
trait PersonServiceAvro[F[_]] {

  def listPersons(empty: Empty.type): F[PersonList]

  def getPerson(id: PersonId): F[Person]

  def getPersonLinks(id: PersonId): F[PersonLinkList]

  def createPerson(person: Person): F[Person]

}
