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

package example.seed.server.protocol.avro

import higherkindness.mu.rpc.protocol._

import shapeless.{:+:, CNil}

sealed trait People extends Product with Serializable

@message final case class Person(name: String, age: Int) extends People

@message final case class NotFoundError(message: String) extends People

@message final case class DuplicatedPersonError(message: String) extends People

@message final case class PeopleRequest(name: String) extends People

@message final case class PeopleResponse(
    result: Person :+: NotFoundError :+: DuplicatedPersonError :+: CNil)
    extends People
