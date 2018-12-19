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

object models {

  type PersonAggregation =
    (Person, Person, Person, Person, PersonLinkList, PersonLinkList, PersonList)

  case class PersonList(persons: List[Person], count: Int) {

    def add(person: Person): PersonList = PersonList(persons = persons :+ person, count + 1)

  }

  case class PersonLinkList(links: List[PersonLink], count: Int)

  case class PersonId(id: String)

  case class Person(
      id: String,
      name: PersonName,
      gender: String,
      location: Location,
      email: String,
      picture: Option[Picture])

  case class PersonName(title: String, first: String, last: String)

  case class Location(street: String, city: String, state: String, postCode: Int)

  case class Picture(large: String, medium: String, thumbnail: String)

  case class PersonLink(p1: Person, p2: Person)

  case class DatabaseException(message: String, maybeCause: Option[Throwable] = None)
      extends RuntimeException(message) {

    maybeCause foreach initCause

  }

}
