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

package foo.bar

import freestyle.rpc.internal.encoders.avro.bigdecimal._
import freestyle.rpc.internal.encoders.avro.javatime._
import freestyle.rpc.protocol._

@message case class HelloRequest(arg1: String, arg2: Option[String], arg3: List[String])

@message case class HelloResponse(arg1: String, arg2: Option[String], arg3: List[String])

@service(AvroWithSchema, Gzip) trait MyGreeterService[F[_]] {

  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]

  def sayNothingAvro(arg: Empty.type): F[Empty.type]

}
