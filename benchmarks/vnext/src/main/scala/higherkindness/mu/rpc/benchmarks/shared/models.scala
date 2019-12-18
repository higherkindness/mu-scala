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

package higherkindness.mu.rpc.benchmarks
package shared

import pbdirect._

object models {

  type PersonAggregation =
    (Person, Person, Person, Person, PersonLinkList, PersonLinkList, PersonList)

  case class PersonList(@pbIndex(1) persons: List[Person], @pbIndex(2) count: Int) {

    def add(person: Person): PersonList = PersonList(persons = persons :+ person, count + 1)

  }

  case class PersonLinkList(@pbIndex(1) links: List[PersonLink], @pbIndex(2) count: Int)

  case class PersonId(@pbIndex(1) id: String)

  case class Person(
      @pbIndex(1) id: String,
      @pbIndex(2) name: PersonName,
      @pbIndex(3) gender: String,
      @pbIndex(4) location: Location,
      @pbIndex(5) email: String,
      @pbIndex(6) picture: Option[Picture])

  case class PersonName(
      @pbIndex(1) title: String,
      @pbIndex(2) first: String,
      @pbIndex(3) last: String)

  case class Location(
      @pbIndex(1) street: String,
      @pbIndex(2) city: String,
      @pbIndex(3) state: String,
      @pbIndex(4) postCode: Int)

  case class Picture(
      @pbIndex(1) large: String,
      @pbIndex(2) medium: String,
      @pbIndex(4) thumbnail: String)

  case class PersonLink(@pbIndex(1) p1: Person, @pbIndex(2) p2: Person)

  case class DatabaseException(message: String, maybeCause: Option[Throwable] = None)
      extends RuntimeException(message) {

    maybeCause foreach initCause

  }

}
