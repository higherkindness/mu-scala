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
import higherkindness.mu.rpc.idlgen.proto.ProtoSrcGenerator
import higherkindness.mu.rpc.common.RpcBaseTestSuite

class ProtoSrcGenTests extends RpcBaseTestSuite {

  val module: String  = new java.io.File(".").getCanonicalPath
  val protoFile: File = new File(module + "/src/test/resources/proto/book.proto")

  "Proto Scala Generator" should {

    "generate correct Scala classes" in {

      val result: Option[(String, Seq[String])] =
        ProtoSrcGenerator
          .generateFrom(inputFile = protoFile, serializationType = "", options = "")
          .map(t => (t._1, t._2.map(_.clean)))

      result shouldBe Some(("com/proto/book.scala", Seq(expectation.clean)))
    }
  }

  val expectation =
    """package com.proto
      |
      |import higherkindness.mu.rpc.protocol._
      |import fs2.Stream
      |import shapeless.{:+:, CNil}
      |import com.proto.author.Author
      |
      |object book {
      |
      |@message final case class Book(isbn: Long, title: String, author: List[Author], binding_type: BindingType)
      |@message final case class GetBookRequest(isbn: Long)
      |@message final case class GetBookViaAuthor(author: Author)
      |@message final case class BookStore(name: String, books: Map[Long, String], genres: List[Genre], payment_method: Long :+: Int :+: String :+: Book :+: CNil)
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
      |@service(Protobuf) trait BookService[F[_]] {
      |  def GetBook(req: GetBookRequest): F[Book]
      |  def GetBooksViaAuthor(req: GetBookViaAuthor): Stream[F, Book]
      |  def GetGreatestBook(req: Stream[F, GetBookRequest]): F[Book]
      |  def GetBooks(req: Stream[F, GetBookRequest]): Stream[F, Book]
      |}
      |
      |}""".stripMargin

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
