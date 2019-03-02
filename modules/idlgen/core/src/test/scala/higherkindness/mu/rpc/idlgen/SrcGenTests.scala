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

package higherkindness.mu.rpc.idlgen

import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.idlgen.AvroScalaGeneratorArbitrary._
import higherkindness.mu.rpc.idlgen.avro._
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.Prop.forAll

class SrcGenTests extends RpcBaseTestSuite with Checkers {

  "Avro Scala Generator" should {

    "generate correct Scala classes" in {
      check {
        forAll { scenario: Scenario =>
          test(scenario)
        }
      }
    }
  }

  private def test(scenario: Scenario): Boolean = {
    val output =
      AvroSrcGenerator(scenario.marshallersImports)
        .generateFrom(
          resource(scenario.inputResourcePath).mkString,
          scenario.serializationType,
          scenario.options: _*)
    output should not be empty
    output forall {
      case (filePath, contents) =>
        filePath shouldBe scenario.expectedOutputFilePath
        contents.toList.filter(_.length > 0) shouldBe scenario.expectedOutput
        true
    }
  }

}
