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

import higherkindness.mu.rpc.benchmarks.shared.models._

import scala.concurrent.duration._

object Utils {

  val defaultTimeOut: FiniteDuration = 30.seconds

  val person: Person = Person(
    id = "5",
    name = PersonName(title = "ms", first = "valentine", last = "lacroix"),
    gender = "female",
    location = Location(
      street = "1494 avenue du fort-caire",
      city = "orléans",
      state = "aveyron",
      postCode = 91831),
    email = "valentine.lacroix@example.com",
    picture = None
  )
}
