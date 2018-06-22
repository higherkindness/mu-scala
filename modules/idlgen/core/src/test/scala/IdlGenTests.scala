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
import freestyle.rpc.idlgen.Model._
import freestyle.rpc.protocol._
import freestyle.rpc.internal.util._

class IdlGenTests extends RpcBaseTestSuite {
  // format: OFF

  import Toolbox.u._

  private def valDef(name: String, tpe: String): ValDef = ValDef(
    Modifiers(NoFlags | Flag.CASEACCESSOR | Flag.PARAMACCESSOR),
    TermName(name),
    Ident(TypeName(tpe)),
    EmptyTree)

  private def valDefHK(name: String, constructor: String, tpe: String): ValDef = ValDef(
    Modifiers(NoFlags | Flag.CASEACCESSOR | Flag.PARAMACCESSOR),
    TermName(name),
    AppliedTypeTree(Ident(TypeName(constructor)), List(Ident(TypeName(tpe)))),
    EmptyTree)

  def `type`(str: String): Tree = str match {
    case x if x.endsWith(".type") => SingletonTypeTree(Ident(TermName(x.stripSuffix(".type"))))
    case x => Ident(TypeName(x))
  }

  val greeterRpcs = RpcDefinitions(
    "MyGreeterService",
    Some("foo.bar"),
    Seq(
      RpcOption("java_multiple_files" , "true"),
      RpcOption("java_outer_classname", "\"Quickstart\"")
    ),
    Seq(
      RpcMessage("HelloRequest",  Seq(valDef("arg1", "String"), valDefHK("arg2", "Option", "String"), valDefHK("arg3", "List", "String"))),
      RpcMessage("HelloResponse", Seq(valDef("arg1", "String"), valDefHK("arg2", "Option", "String"), valDefHK("arg3", "List", "String")))
    ),
    Seq(RpcService("Greeter", Seq(
      RpcRequest(Avro          , "sayHelloAvro"        , `type`("HelloRequest"), `type`("HelloResponse"), None),
      RpcRequest(AvroWithSchema, "sayHelloAvro"        , `type`("HelloRequest"), `type`("HelloResponse"), None),
      RpcRequest(Protobuf      , "sayHelloProto"       , `type`("HelloRequest"), `type`("HelloResponse"), None),
      RpcRequest(Avro          , "sayNothingAvro"      , `type`("Empty.type")  , `type`("Empty.type")   , None),
      RpcRequest(AvroWithSchema, "sayNothingAvro"      , `type`("Empty.type")  , `type`("Empty.type")   , None),
      RpcRequest(Protobuf      , "sayNothingProto"     , `type`("Empty.type")  , `type`("Empty.type")   , None),
      RpcRequest(Avro          , "lotsOfRepliesAvro"   , `type`("HelloRequest"), `type`("HelloResponse"), Some(ResponseStreaming)),
      RpcRequest(AvroWithSchema, "lotsOfRepliesAvro"   , `type`("HelloRequest"), `type`("HelloResponse"), Some(ResponseStreaming)),
      RpcRequest(Protobuf      , "lotsOfRepliesProto"  , `type`("HelloRequest"), `type`("HelloResponse"), Some(ResponseStreaming)),
      RpcRequest(Avro          , "lotsOfGreetingsAvro" , `type`("HelloRequest"), `type`("HelloResponse"), Some(RequestStreaming)),
      RpcRequest(AvroWithSchema, "lotsOfGreetingsAvro" , `type`("HelloRequest"), `type`("HelloResponse"), Some(RequestStreaming)),
      RpcRequest(Protobuf      , "lotsOfGreetingsProto", `type`("HelloRequest"), `type`("HelloResponse"), Some(RequestStreaming)),
      RpcRequest(Avro          , "bidiHelloAvro"       , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming)),
      RpcRequest(AvroWithSchema, "bidiHelloAvro"       , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming)),
      RpcRequest(Protobuf      , "bidiHelloProto"      , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming)),
      RpcRequest(Avro          , "bidiHelloFs2Avro"    , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming)),
      RpcRequest(AvroWithSchema, "bidiHelloFs2Avro"    , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming)),
      RpcRequest(Protobuf      , "bidiHelloFs2Proto"   , `type`("HelloRequest"), `type`("HelloResponse"), Some(BidirectionalStreaming))
    )))
  )

  "Scala Parser" should {
    "generate correct RPC definitions from Scala source file" in {
      val input = Toolbox.parse(resource("/GreeterService.scala").mkString)
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
