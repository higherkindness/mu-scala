/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.srcgen

import higherkindness.mu.rpc.srcgen.Model._
import org.scalacheck.{Arbitrary, Gen}

trait AvroScalaGeneratorArbitrary {

  case class Scenario(
      inputResourcePath: String,
      expectedOutput: List[String],
      expectedOutputFilePath: String,
      serializationType: String,
      marshallersImports: List[MarshallersImport],
      options: Seq[String],
      compressionTypeGen: CompressionTypeGen,
      useIdiomaticEndpoints: UseIdiomaticEndpoints)

  def generateOutput(
      serializationType: String,
      marshallersImports: List[MarshallersImport],
      options: Option[String],
      compressionTypeGen: CompressionTypeGen,
      useIdiomaticEndpoints: UseIdiomaticEndpoints): List[String] = {

    val imports: String = ("import higherkindness.mu.rpc.protocol._" :: marshallersImports
      .map(_.marshallersImport)
      .map("import " + _)).sorted
      .mkString("\n")

    val serviceParams: Seq[String] =
      if (options.isEmpty) {
        serializationType ::
          s"compressionType = ${compressionTypeGen.value}" ::
          List(s"""namespace = Some("foo.bar")""", "methodNameStyle = Capitalize").filter(_ =>
          useIdiomaticEndpoints)
      } else serializationType +: options.toSeq

    s"""
         |package foo.bar
         |
         |$imports
         |
         |@message final case class HelloRequest(arg1: String, arg2: Option[String], arg3: Seq[String])
         |
         |@message final case class HelloResponse(arg1: String, arg2: Option[String], arg3: Seq[String])
         |
         |@service(${serviceParams.mkString(",")}) trait MyGreeterService[F[_]] {
         |
         |  def sayHelloAvro(arg: foo.bar.HelloRequest): F[foo.bar.HelloResponse]
         |
         |  def sayNothingAvro(arg: Empty.type): F[Empty.type]
         |
         |}""".stripMargin.split("\n").filter(_.length > 0).toList
  }

  val importSliceGen: Gen[String] =
    Gen.choose(4, 10).flatMap(Gen.listOfN(_, Gen.alphaLowerChar).map(_.mkString("")))

  val customMarshallersImportsGen: Gen[MarshallersImport] =
    Gen
      .choose(1, 5)
      .flatMap(
        Gen.listOfN(_, importSliceGen).map(_.mkString(".") + "._").map(CustomMarshallersImport))

  def marshallersImportGen(serializationType: String): Gen[MarshallersImport] =
    serializationType match {
      case "Avro" | "AvroWithSchema" =>
        Gen.oneOf(
          Gen.const(BigDecimalAvroMarshallers),
          Gen.const(JodaDateTimeAvroMarshallers),
          customMarshallersImportsGen
        )
      case "Protobuf" =>
        Gen.oneOf(
          Gen.const(BigDecimalProtobufMarshallers),
          Gen.const(JavaTimeDateAvroMarshallers),
          Gen.const(JavaTimeDateProtobufMarshallers),
          Gen.const(JodaDateTimeProtobufMarshallers),
          customMarshallersImportsGen
        )
      case _ => customMarshallersImportsGen
    }

  implicit val scenarioArb: Arbitrary[Scenario] = Arbitrary {
    for {
      inputResourcePath     <- Gen.oneOf("/avro/GreeterService.avpr", "/avro/GreeterService.avdl")
      serializationType     <- Gen.oneOf("Avro", "AvroWithSchema", "Protobuf")
      marshallersImports    <- Gen.listOf(marshallersImportGen(serializationType))
      options               <- Gen.option("Gzip")
      compressionTypeGen    <- Gen.oneOf(GzipGen, NoCompressionGen)
      useIdiomaticEndpoints <- Arbitrary.arbBool.arbitrary.map(UseIdiomaticEndpoints(_))
    } yield
      Scenario(
        inputResourcePath,
        generateOutput(
          serializationType,
          marshallersImports,
          options,
          compressionTypeGen,
          useIdiomaticEndpoints),
        "foo/bar/MyGreeterService.scala",
        serializationType,
        marshallersImports,
        options.toSeq,
        compressionTypeGen,
        useIdiomaticEndpoints
      )
  }

}

object AvroScalaGeneratorArbitrary extends AvroScalaGeneratorArbitrary
