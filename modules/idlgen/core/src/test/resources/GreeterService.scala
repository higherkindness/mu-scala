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

import freestyle.rpc.protocol._

@outputName("MyGreeterService")
@outputPackage("foo.bar")
@option("java_multiple_files", true)
@option(name = "java_outer_classname", value = "Quickstart")
object GreeterService {

  @message case class HelloRequest(arg1: String, arg2: Option[String], arg3: List[String])

  @message case class HelloResponse(arg1: String, arg2: Option[String], arg3: List[String])

  @service trait Greeter[F[_]] {

    @rpc(Avro)
    def sayHelloAvro(request: HelloRequest): F[HelloResponse]

    @rpc(Protobuf)
    def sayHelloProto(request: HelloRequest): F[HelloResponse]

    @rpc(Avro)
    def sayNothingAvro(request: Empty.type): F[Empty.type]

    @rpc(Protobuf)
    def sayNothingProto(request: Empty.type): F[Empty.type]

    @rpc(Avro)
    @stream[ResponseStreaming.type]
    def lotsOfRepliesAvro(request: HelloRequest): Observable[HelloResponse]

    @rpc(Protobuf)
    @stream[ResponseStreaming.type]
    def lotsOfRepliesProto(request: HelloRequest): Observable[HelloResponse]

    @rpc(Avro)
    @stream[RequestStreaming.type]
    def lotsOfGreetingsAvro(request: Observable[HelloRequest]): F[HelloResponse]

    @rpc(Protobuf)
    @stream[RequestStreaming.type]
    def lotsOfGreetingsProto(request: Observable[HelloRequest]): F[HelloResponse]

    @rpc(Avro)
    @stream[BidirectionalStreaming.type]
    def bidiHelloAvro(request: Observable[HelloRequest]): Observable[HelloResponse]

    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHelloProto(request: Observable[HelloRequest]): Observable[HelloResponse]

    @rpc(Avro)
    @stream[BidirectionalStreaming.type]
    def bidiHelloFs2Avro(request: Stream[F, HelloRequest]): Stream[F, HelloResponse]

    @rpc(Protobuf)
    @stream[BidirectionalStreaming.type]
    def bidiHelloFs2Proto(request: Stream[F, HelloRequest]): Stream[F, HelloResponse]

  }

}
