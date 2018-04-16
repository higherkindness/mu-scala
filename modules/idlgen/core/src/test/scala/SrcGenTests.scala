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
import freestyle.rpc.idlgen.avro._

class SrcGenTests extends RpcBaseTestSuite {

  "Avro Scala Generator" should {
    "generate correct Scala classes from .avpr" in
      test(
        "/avro/GreeterService.avpr",
        "/avro/MyGreeterService.scala",
        "foo/bar/MyGreeterService.scala")

    "generate correct Scala classes from .avdl" in
      test(
        "/avro/GreeterService.avdl",
        "/avro/MyGreeterService.scala",
        "foo/bar/MyGreeterService.scala")

    "generate correct Scala classes from .avdl for AvroWithSchema serialization type" in
      test(
        "/avro/GreeterService.avdl",
        "/avro/MyGreeterWithSchemaService.scala",
        "foo/bar/MyGreeterService.scala",
        "AvroWithSchema")
  }

  private def test(
      inputResourcePath: String,
      outputResourcePath: String,
      outputFilePath: String,
      serializationType: String = "Avro"): Unit = {
    val expectedOutput = resource(outputResourcePath).getLines.toList
      .dropWhile(line => line.startsWith("/*") || line.startsWith(" *"))
      .tail
    val output =
      AvroSrcGenerator.generateFrom(resource(inputResourcePath).mkString, serializationType, "Gzip")
    output should not be empty
    val (filePath, contents) = output.get
    filePath shouldBe outputFilePath
    contents.toList shouldBe expectedOutput
  }

}
