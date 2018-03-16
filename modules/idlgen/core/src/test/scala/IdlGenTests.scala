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
import freestyle.rpc.protocol._
import org.scalactic._
import scala.compat.Platform
import scala.io
import scala.io.BufferedSource
import scala.meta._

class IdlGenTests extends RpcBaseTestSuite {
  // format: OFF
  implicit private val prettifier = Prettifier { case x: Any =>
    Platform.EOL + Prettifier.default(x) // initial linebreak makes expected/actual results line up nicely
  }

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
      RpcRequest(Avro    , "sayHelloAvro"        , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Protobuf, "sayHelloProto"       , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Avro    , "sayNothingAvro"      , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(Protobuf, "sayNothingProto"     , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(Avro    , "lotsOfRepliesAvro"   , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(Protobuf, "lotsOfRepliesProto"  , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(Avro    , "lotsOfGreetingsAvro" , t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(Protobuf, "lotsOfGreetingsProto", t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(Avro    , "bidiHelloAvro"       , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Protobuf, "bidiHelloProto"      , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Avro    , "bidiHelloFs2Avro"    , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming)),
      RpcRequest(Protobuf, "bidiHelloFs2Proto"   , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming))
    )))
  )

  "Parser.parse()" should {
    "generate correct RPC definitions from Scala source file" in {
      val input = resource("/GreeterService.scala").mkString.parse[Source].get
      val RpcDefinitions(pkg, name, options, messages, services) = Parser.parse(input, "GreeterService")
      val RpcDefinitions(expectedPkg, expectedName, expectedOptions, expectedMessages, expectedServices) = greeterRpcs
      pkg shouldBe expectedPkg
      name shouldBe expectedName
      options shouldBe expectedOptions
      messages shouldBe expectedMessages
      services shouldBe expectedServices
    }
  }

  "$Generator.generateFrom()" should {
    "generate correct Protobuf syntax from RPC definitions" in {
      val expected = resource("/proto/GreeterService.proto").getLines.toList
      val output = Generator.generateFrom(greeterRpcs)
      output.get(ProtoGenerator) should not be empty
      output(ProtoGenerator).toList shouldBe expected
    }
  }

  "Generator.generateFrom()" should {
    "generate correct Avro syntax from RPC definitions" in {
      val expected = resource("/avro/GreeterService.avpr").getLines.toList
      val output = Generator.generateFrom(greeterRpcs)
      output.get(AvroGenerator) should not be empty
      output(AvroGenerator).toList shouldBe expected
    }
  }

  private def resource(path: String): BufferedSource = io.Source.fromInputStream(getClass.getResourceAsStream(path))

  // format: ON
}
