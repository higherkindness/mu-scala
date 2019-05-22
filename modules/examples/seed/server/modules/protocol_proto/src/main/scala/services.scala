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

package example.seed.server.protocol.proto

import higherkindness.mu.rpc.protocol._
import fs2.Stream
import shapeless.{:+:, CNil}

import example.seed.server.protocol.proto.people.PeopleRequest
import example.seed.server.protocol.proto.people.PeopleResponse

object services {

  @service(Protobuf) trait PeopleService[F[_]] {
    def getPerson(req: PeopleRequest): F[PeopleResponse]
    def getPersonStream(req: Stream[F, PeopleRequest]): Stream[F, PeopleResponse]
  }

}
