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

import java.io.File

import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.idlgen.proto.ProtoSrcGenerator
import higherkindness.mu.rpc.idlgen.Model.{NoCompressionGen, UseIdiomaticEndpoints}
import org.scalatest.OptionValues

class ProtoSrcGenTests extends RpcBaseTestSuite with OptionValues {

  val module: String = new java.io.File(".").getCanonicalPath
  def protoFile(filename: String): File =
    new File(s"$module/src/test/resources/proto/$filename.proto")

  "Proto Scala Generator" should {

    "generate correct Scala classes" in {

      val result: Option[(String, String)] =
        ProtoSrcGenerator
          .build(NoCompressionGen, UseIdiomaticEndpoints(false), new java.io.File("."))
          .generateFrom(files = Set(protoFile("book")), serializationType = "", options = "")
          .map(t => (t._2, t._3.mkString("\n").clean))
          .headOption

      result shouldBe Some(("com/proto/book.scala", bookExpectation.clean))
    }

    case class ImportsTestCase(
        protoFilename: String,
        shouldIncludeFS2Import: Boolean,
        shouldIncludeShapelessImport: Boolean
    )

    for (test <- List(
        ImportsTestCase("streaming_no_shapeless_no", false, false),
        ImportsTestCase("streaming_yes_shapeless_no", true, false),
        ImportsTestCase("streaming_no_shapeless_yes", false, true),
        ImportsTestCase("streaming_yes_shapeless_yes", true, true)
      )) {

      s"include the correct imports (${test.protoFilename})" in {

        val result: Option[String] =
          ProtoSrcGenerator
            .build(NoCompressionGen, UseIdiomaticEndpoints(false), new java.io.File("."))
            .generateFrom(
              files = Set(protoFile(test.protoFilename)),
              serializationType = "",
              options = "")
            .map(_._3.mkString("\n"))
            .headOption

        assert(result.value.contains("import fs2.") == test.shouldIncludeFS2Import)
        assert(result.value.contains("import shapeless.") == test.shouldIncludeShapelessImport)
      }

    }

  }

  val bookExpectation =
    """package com.proto
      |
      |import higherkindness.mu.rpc.protocol._
      |import fs2.Stream
      |import shapeless.{:+:, CNil}
      |
      |object book {
      |
      |@message final case class Book(
      |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long,
      |  @_root_.pbdirect.pbIndex(2) title: _root_.java.lang.String,
      |  @_root_.pbdirect.pbIndex(3) author: _root_.scala.List[_root_.scala.Option[_root_.com.proto.author.Author]],
      |  @_root_.pbdirect.pbIndex(9) binding_type: _root_.scala.Option[_root_.com.proto.book.BindingType]
      |)
      |@message final case class GetBookRequest(
      |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long
      |)
      |@message final case class GetBookViaAuthor(
      |  @_root_.pbdirect.pbIndex(1) author: _root_.scala.Option[_root_.com.proto.author.Author]
      |)
      |@message final case class BookStore(
      |  @_root_.pbdirect.pbIndex(1) name: _root_.java.lang.String,
      |  @_root_.pbdirect.pbIndex(2) books: _root_.scala.Map[_root_.scala.Long, _root_.java.lang.String],
      |  @_root_.pbdirect.pbIndex(3) genres: _root_.scala.List[_root_.scala.Option[_root_.com.proto.book.Genre]],
      |  @_root_.pbdirect.pbIndex(4,5,6,7) payment_method: _root_.scala.Long :+: _root_.scala.Int :+: _root_.java.lang.String :+: _root_.com.proto.book.Book :+: CNil
      |)
      |
      |sealed abstract class Genre(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |object Genre extends _root_.enumeratum.values.IntEnum[Genre] {
      |  case object UNKNOWN extends Genre(0)
      |  case object SCIENCE_FICTION extends Genre(1)
      |  case object POETRY extends Genre(2)
      |
      |  val values = findValues
      |}
      |
      |
      |sealed abstract class BindingType(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |object BindingType extends _root_.enumeratum.values.IntEnum[BindingType] {
      |  case object HARDCOVER extends BindingType(0)
      |  case object PAPERBACK extends BindingType(1)
      |
      |  val values = findValues
      |}
      |
      |@service(Protobuf,Identity) trait BookService[F[_]] {
      |  def GetBook(req: _root_.com.proto.book.GetBookRequest): F[_root_.com.proto.book.Book]
      |  def GetBooksViaAuthor(req: _root_.com.proto.book.GetBookViaAuthor): Stream[F, _root_.com.proto.book.Book]
      |  def GetGreatestBook(req: Stream[F, _root_.com.proto.book.GetBookRequest]): F[_root_.com.proto.book.Book]
      |  def GetBooks(req: Stream[F, _root_.com.proto.book.GetBookRequest]): Stream[F, _root_.com.proto.book.Book]
      |}
      |
      |}""".stripMargin

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
