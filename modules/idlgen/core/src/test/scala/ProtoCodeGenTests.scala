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

package freestyle.rpc.idlgen.protobuf

import freestyle.rpc.common.RpcBaseTestSuite
import java.io.File
import scala.io.Source

class ProtoCodeGenTests extends RpcBaseTestSuite {

  s"$ProtoCodeGen.generate()" should {
    "generate a correct .proto file" in {
      // Clunky workaround for Travis CI env until we refactor Protogen to use classpath-provided streams
      val baseDir = Option(System.getenv("TRAVIS_BUILD_DIR")).fold(".")(travisDir =>
        s"$travisDir/frees-io/freestyle-rpc/modules/idlgen/core") + "/src/test"
      val srcDir        = s"$baseDir/scala"
      val dstDir        = s"$baseDir/proto"
      val output        = ProtoCodeGen.generate(new File(srcDir), new File(dstDir)).flatten
      val greeterOutput = output.find(_._1 == new File(s"$dstDir/GreeterService.proto"))
      greeterOutput should not be empty
      val outputLines = greeterOutput.get._2.split("\n")
      val expectedLines =
        Source.fromFile(s"$dstDir/GreeterService_Expected.proto", "UTF-8").getLines.toArray
      // initial line breaks make it easier to compare the arrays in ScalaTest assertion errors
      "\n" +: trimmed(outputLines) shouldBe "\n" +: trimmed(expectedLines)
    }
  }

  private def trimmed(output: Array[String]) = output.map(_.trim).filter(_.nonEmpty)

}
