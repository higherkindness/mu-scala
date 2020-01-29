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

import java.io.File

import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.srcgen.proto.ProtoSrcGenerator
import higherkindness.mu.rpc.srcgen.Model.{
  Fs2Stream,
  MonixObservable,
  NoCompressionGen,
  SerializationType,
  UseIdiomaticEndpoints
}
import org.scalatest.OptionValues

class ProtoSrcGenTests extends RpcBaseTestSuite with OptionValues {

  val module: String = new java.io.File(".").getCanonicalPath
  def protoFile(filename: String): File =
    new File(s"$module/src/test/resources/proto/$filename.proto")

  "Proto Scala Generator" should {

    "generate the expected Scala code (FS2 stream)" in {
      val result: Option[(String, String)] =
        ProtoSrcGenerator
          .build(NoCompressionGen, UseIdiomaticEndpoints(false), Fs2Stream, new java.io.File("."))
          .generateFrom(
            files = Set(protoFile("book")),
            serializationType = SerializationType.Protobuf
          )
          .map(t => (t._2, t._3.mkString("\n").clean))
          .headOption

      val expectedFileContent = bookExpectation(tpe => s"_root_.fs2.Stream[F, $tpe]").clean
      result shouldBe Some(("com/proto/book.scala", expectedFileContent.clean))
    }

    "generate the expected Scala code (Monix Observable)" in {
      val result: Option[(String, String)] =
        ProtoSrcGenerator
          .build(
            NoCompressionGen,
            UseIdiomaticEndpoints(false),
            MonixObservable,
            new java.io.File(".")
          )
          .generateFrom(
            files = Set(protoFile("book")),
            serializationType = SerializationType.Protobuf
          )
          .map(t => (t._2, t._3.mkString("\n").clean))
          .headOption

      val expectedFileContent =
        bookExpectation(tpe => s"_root_.monix.reactive.Observable[$tpe]").clean

      result shouldBe Some(("com/proto/book.scala", expectedFileContent))
    }

  }

  def bookExpectation(streamOf: String => String): String =
    s"""package com.proto
      |
      |import _root_.higherkindness.mu.rpc.protocol._
      |
      |object book {
      |
      |@message final case class Book(
      |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long,
      |  @_root_.pbdirect.pbIndex(2) title: _root_.java.lang.String,
      |  @_root_.pbdirect.pbIndex(3) author: _root_.scala.List[_root_.com.proto.author.Author],
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
      |  @_root_.pbdirect.pbIndex(2) books: _root_.scala.Predef.Map[_root_.scala.Long, _root_.java.lang.String],
      |  @_root_.pbdirect.pbIndex(3) genres: _root_.scala.List[_root_.com.proto.book.Genre],
      |  @_root_.pbdirect.pbIndex(4,5,6,7) payment_method: _root_.scala.Option[
      |    _root_.shapeless.:+:[
      |      _root_.scala.Long,
      |      _root_.shapeless.:+:[
      |        _root_.scala.Int,
      |        _root_.shapeless.:+:[
      |          _root_.java.lang.String,
      |          _root_.shapeless.:+:[
      |            _root_.com.proto.book.Book,
      |            _root_.shapeless.CNil]]]]]
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
      |sealed abstract class BindingType(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |object BindingType extends _root_.enumeratum.values.IntEnum[BindingType] {
      |  case object HARDCOVER extends BindingType(0)
      |  case object PAPERBACK extends BindingType(1)
      |
      |  val values = findValues
      |}
      |
      |@service(Protobuf, Identity) trait BookService[F[_]] {
      |  def GetBook(req: _root_.com.proto.book.GetBookRequest): F[_root_.com.proto.book.Book]
      |  def GetBooksViaAuthor(req: _root_.com.proto.book.GetBookViaAuthor): ${streamOf(
         "_root_.com.proto.book.Book"
       )}
      |  def GetGreatestBook(req: ${streamOf("_root_.com.proto.book.GetBookRequest")}): F[_root_.com.proto.book.Book]
      |  def GetBooks(req: ${streamOf("_root_.com.proto.book.GetBookRequest")}): ${streamOf(
         "_root_.com.proto.book.Book"
       )}
      |}
      |
      |}""".stripMargin

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
