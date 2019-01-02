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

import higherkindness.mu.rpc.benchmarks.shared.models._

object database {

  val p1: Person = Person(
    id = "1",
    name = PersonName(title = "mr", first = "romain", last = "hoogmoed"),
    gender = "male",
    location = Location(
      street = "1861 jan pieterszoon coenstraat",
      city = "maasdriel",
      state = "zeeland",
      postCode = 69217),
    email = "romain.hoogmoed@example.com",
    picture = None
  )

  val p2: Person = Person(
    id = "2",
    name = PersonName(title = "mademoiselle", first = "morgane", last = "lefebvre"),
    gender = "female",
    location = Location(
      street = "2260 rue de gerland",
      city = "vucherens",
      state = "graubünden",
      postCode = 2877),
    email = "morgane.lefebvre@example.com",
    picture = Some(
      Picture(
        large = "https://randomuser.me/api/portraits/women/62.jpg",
        medium = "https://randomuser.me/api/portraits/med/women/62.jpg",
        thumbnail = "https://randomuser.me/api/portraits/thumb/women/62.jpg"
      ))
  )

  val p3: Person = Person(
    id = "3",
    name = PersonName(title = "ms", first = "eva", last = "snyder"),
    gender = "female",
    location =
      Location(street = "8534 grove road", city = "mallow", state = "clare", postCode = 18863),
    email = "eva.snyder@example.com",
    picture = Some(
      Picture(
        large = "https://randomuser.me/api/portraits/women/84.jpg",
        medium = "https://randomuser.me/api/portraits/med/women/84.jpg",
        thumbnail = "https://randomuser.me/api/portraits/thumb/women/84.jpg"
      ))
  )

  val p4: Person = Person(
    id = "4",
    name = PersonName(title = "monsieur", first = "elliot", last = "bertrand"),
    gender = "male",
    location = Location(
      street = "2557 rue abel",
      city = "tolochenaz",
      state = "graubünden",
      postCode = 4629),
    email = "elliot.bertrand@example.com",
    picture = Some(
      Picture(
        large = "https://randomuser.me/api/portraits/men/30.jpg",
        medium = "https://randomuser.me/api/portraits/med/men/30.jpg",
        thumbnail = "https://randomuser.me/api/portraits/thumb/men/30.jpg"
      ))
  )

  val persons: List[Person] = List(p1, p2, p3, p4)

  val personLinks: List[PersonLink] = List(
    PersonLink(p1, p2),
    PersonLink(p1, p3),
    PersonLink(p2, p3),
    PersonLink(p4, p1)
  )

}
