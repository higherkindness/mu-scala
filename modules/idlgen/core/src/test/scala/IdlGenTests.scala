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

package freestyle.rpc.idlgen

import freestyle.rpc.common.RpcBaseTestSuite
import freestyle.rpc.idlgen.avro.{AvroIdlGenerator, AvroWithSchemaIdlGenerator}
import freestyle.rpc.idlgen.proto.ProtoIdlGenerator
import freestyle.rpc.protocol._
import scala.meta._

class IdlGenTests extends RpcBaseTestSuite {
  // format: OFF

  val greeterRpcs = RpcDefinitions(
    "MyGreeterService",
    Some("foo.bar"),
    Seq(
      RpcOption("java_multiple_files" , "true"),
      RpcOption("java_outer_classname", "\"Quickstart\"")
    ),
    Seq(
      RpcMessage("HelloRequest",  Seq(param"arg1: String", param"arg2: Option[String]", param"arg3: List[String]")),
      RpcMessage("HelloResponse", Seq(param"arg1: String", param"arg2: Option[String]", param"arg3: List[String]"))
    ),
    Seq(RpcService("Greeter", Seq(
      RpcRequest(Avro          , "sayHelloAvro"        , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(AvroWithSchema, "sayHelloAvro"        , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Protobuf      , "sayHelloProto"       , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Avro          , "sayNothingAvro"      , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(AvroWithSchema, "sayNothingAvro"      , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(Protobuf      , "sayNothingProto"     , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(Avro          , "lotsOfRepliesAvro"   , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(AvroWithSchema, "lotsOfRepliesAvro"   , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(Protobuf      , "lotsOfRepliesProto"  , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(Avro          , "lotsOfGreetingsAvro" , t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(AvroWithSchema, "lotsOfGreetingsAvro" , t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(Protobuf      , "lotsOfGreetingsProto", t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(Avro          , "bidiHelloAvro"       , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(AvroWithSchema, "bidiHelloAvro"       , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Protobuf      , "bidiHelloProto"      , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Avro          , "bidiHelloFs2Avro"    , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(AvroWithSchema, "bidiHelloFs2Avro"    , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Protobuf      , "bidiHelloFs2Proto"   , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming))
    )))
  )

  "Scala Parser" should {
    "generate correct RPC definitions from Scala source file" in {
      val input = resource("/GreeterService.scala").mkString.parse[Source].get
      val RpcDefinitions(pkg, name, options, messages, services) = ScalaParser.parse(input, "GreeterService")
      val RpcDefinitions(expectedPkg, expectedName, expectedOptions, expectedMessages, expectedServices) = greeterRpcs
      pkg shouldBe expectedPkg
      name shouldBe expectedName
      options shouldBe expectedOptions
      messages shouldBe expectedMessages
      services shouldBe expectedServices
    }
  }

  "Proto IDL Generator" should {
    "generate correct Protobuf syntax from RPC definitions" in {
      val expected = resource("/proto/GreeterService.proto").getLines.toList
      val output = ProtoIdlGenerator.generateFrom(greeterRpcs)
      output should not be empty
      output.get.toList shouldBe expected
    }
  }

  "Avro IDL Generator" should {
    "generate correct Avro syntax from RPC definitions" in {
      val expected = resource("/avro/GreeterService.avpr").getLines.toList
      val output = AvroIdlGenerator.generateFrom(greeterRpcs)
      output should not be empty
      output.get.toList shouldBe expected
    }
  }

  "Avro With Schema IDL Generator" should {
    "generate correct Avro syntax from RPC definitions" in {
      val expected = resource("/avro/GreeterService.avpr").getLines.toList
      val output = AvroWithSchemaIdlGenerator.generateFrom(greeterRpcs)
      output should not be empty
      output.get.toList shouldBe expected
    }
  }

  // format: ON
}
