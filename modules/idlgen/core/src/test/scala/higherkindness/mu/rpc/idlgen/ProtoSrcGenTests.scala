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
import higherkindness.mu.rpc.idlgen.Model.{Fs2Stream, NoCompressionGen, UseIdiomaticEndpoints}
import org.scalatest.OptionValues

class ProtoSrcGenTests extends RpcBaseTestSuite with OptionValues {

  val module: String = new java.io.File(".").getCanonicalPath
  def protoFile(filename: String): File =
    new File(s"$module/src/test/resources/proto/$filename.proto")

  "Proto Scala Generator" should {

    "generate correct Scala classes" in {

      val result: Option[(String, String)] =
        ProtoSrcGenerator
          .build(NoCompressionGen, UseIdiomaticEndpoints(false), Fs2Stream, new java.io.File("."))
          .generateFrom(files = Set(protoFile("book")), serializationType = "", options = "")
          .map(t => (t._2, t._3.mkString("\n").clean))
          .headOption

      result shouldBe Some(("com/proto/book.scala", bookExpectation.clean))
    }

    case class ImportsTestCase(
        protoFilename: String,
        shouldIncludeShapelessImport: Boolean
    )

    for (test <- List(
        ImportsTestCase("shapeless_no", false),
        ImportsTestCase("shapeless_yes", true),
      )) {

      s"include the correct imports (${test.protoFilename})" in {

        val result: Option[String] =
          ProtoSrcGenerator
            .build(NoCompressionGen, UseIdiomaticEndpoints(false), Fs2Stream, new java.io.File("."))
            .generateFrom(
              files = Set(protoFile(test.protoFilename)),
              serializationType = "",
              options = "")
            .map(_._3.mkString("\n"))
            .headOption

        assert(result.value.contains("import shapeless.") == test.shouldIncludeShapelessImport)
      }

    }

  }

  val bookExpectation =
    """package com.proto
      |
      |import higherkindness.mu.rpc.protocol._
      |import shapeless.{:+:, CNil}
      |import com.proto.author.Author
      |
      |object book {
      |
      |@message final case class Book(isbn: Long, title: String, author: List[Option[Author]], binding_type: Option[BindingType])
      |@message final case class GetBookRequest(isbn: Long)
      |@message final case class GetBookViaAuthor(author: Option[Author])
      |@message final case class BookStore(name: String, books: Map[Long, String], genres: List[Option[Genre]], payment_method: Long :+: Int :+: String :+: Book :+: CNil)
      |
      |sealed trait Genre
      |object Genre {
      |  case object UNKNOWN extends Genre
      |  case object SCIENCE_FICTION extends Genre
      |  case object POETRY extends Genre
      |}
      |
      |
      |sealed trait BindingType
      |object BindingType {
      |  case object HARDCOVER extends BindingType
      |  case object PAPERBACK extends BindingType
      |}
      |
      |@service(Protobuf,Identity) trait BookService[F[_]] {
      |  def GetBook(req: GetBookRequest): F[Book]
      |  def GetBooksViaAuthor(req: GetBookViaAuthor): _root_.fs2.Stream[F, Book]
      |  def GetGreatestBook(req: _root_.fs2.Stream[F, GetBookRequest]): F[Book]
      |  def GetBooks(req: _root_.fs2.Stream[F, GetBookRequest]): _root_.fs2.Stream[F, Book]
      |}
      |
      |}""".stripMargin

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
