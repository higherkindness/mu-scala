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

package higherkindness.mu.rpc

import pbdirect._

object testmodels {

  case class A(@pbIndex(1) x: Int, @pbIndex(2) y: Int)

  case class B(@pbIndex(1) a1: A, @pbIndex(2) a2: A)

  case class C(@pbIndex(1) foo: String, @pbIndex(2) a: A)

  case class D(@pbIndex(1) bar: Int)

  case class E(@pbIndex(1) a: A, @pbIndex(2) foo: String)

  object ExternalScope {
    case class External(@pbIndex(1) e: E)
  }

}
