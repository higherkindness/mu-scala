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

package higherkindness.mu.rpc.http

import higherkindness.mu.rpc.protocol._

@message final case class HelloRequest(hello: String)

@message final case class HelloResponse(hello: String)

// We don't actually need to split the various streaming types into their own services,
// but this allows for more specific dependencies and type constraints (Sync, Async, Effect...) in their implementations.

@service(Avro) trait UnaryGreeter[F[_]] {

  @http(GET, "getHello") def getHello(request: Empty.type): F[HelloResponse]

  @http(POST, "sayHello") def sayHello(request: HelloRequest): F[HelloResponse]
}

import fs2.Stream
@service(Avro) trait Fs2Greeter[F[_]] {

  def sayHellos(requests: Stream[F, HelloRequest]): F[HelloResponse]

  def sayHelloAll(request: HelloRequest): Stream[F, HelloResponse]

  def sayHellosAll(requests: Stream[F, HelloRequest]): Stream[F, HelloResponse]
}

import monix.reactive.Observable
@service(Avro) trait MonixGreeter[F[_]] {

  def sayHellos(requests: Observable[HelloRequest]): F[HelloResponse]

  def sayHelloAll(request: HelloRequest): Observable[HelloResponse]

  def sayHellosAll(requests: Observable[HelloRequest]): Observable[HelloResponse]
}
