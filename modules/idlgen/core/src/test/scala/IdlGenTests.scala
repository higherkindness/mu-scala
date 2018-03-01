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
import scala.meta._

class IdlGenTests extends RpcBaseTestSuite {
  // format: OFF
  implicit private val prettifier = Prettifier { case x: Any =>
    Platform.EOL + Prettifier.default(x) // initial linebreak makes expected/actual results line up nicely
  }

  val greeterRpcs = RpcDefinitions(
    Seq(
      RpcOption("java_package"        , "quickstart", quote = true),
      RpcOption("java_multiple_files" , "true",       quote = false),
      RpcOption("java_outer_classname", "Quickstart", quote = true)
    ),
    Seq(
      RpcMessage("HelloRequest",  Seq(param"greeting: String")),
      RpcMessage("HellosRequest", Seq(param"greetings: List [String]")),
      RpcMessage("HelloResponse", Seq(param"reply: String"))
    ),
    Seq(
    RpcService("Greeter", Seq(
      RpcRequest(Avro,     "sayHelloAvro"   , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Protobuf, "sayHelloProto"  , t"HelloRequest", t"HelloResponse", None),
      RpcRequest(Protobuf, "sayNothing"     , t"Empty.type"  , t"Empty.type"   , None),
      RpcRequest(Protobuf, "lotsOfReplies"  , t"HelloRequest", t"HelloResponse", Some(ResponseStreaming)),
      RpcRequest(Protobuf, "lotsOfGreetings", t"HelloRequest", t"HelloResponse", Some(RequestStreaming)),
      RpcRequest(Protobuf, "bidiHello"      , t"HelloRequest", t"HelloResponse", Some(BidirectionalStreaming))
    )))
  )

  s"${Parser.getClass.getSimpleName}.parse()" should {
    "generate correct RPC definitions from Scala source file" in {
      val input = io.Source.fromResource("GreeterService.scala").mkString.parse[Source].get
      val RpcDefinitions(options, messages, services) = Parser.parse(input)
      val RpcDefinitions(expectedOptions, expectedMessages, expectedServices) = greeterRpcs
      options shouldBe expectedOptions
      messages shouldBe expectedMessages
      services shouldBe expectedServices
    }
  }

  s"${Generator.getClass.getSimpleName}.generateFrom()" should {
    "generate correct IDL syntax from RPC definitions" in {
      val expectedProto = io.Source.fromResource("proto/GreeterService.proto").getLines.toList
      val output = Generator.generateFrom(greeterRpcs)
      output.get(ProtoGenerator) should not be empty
      output(ProtoGenerator) shouldBe expectedProto
    }
  }
  // format: ON
}
