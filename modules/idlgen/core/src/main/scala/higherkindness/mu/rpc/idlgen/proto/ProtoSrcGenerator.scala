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

package higherkindness.mu.rpc.idlgen.proto

import java.io.File

import cats.effect.{IO, Sync}
import cats.syntax.functor._
import higherkindness.mu.rpc.idlgen._
import higherkindness.skeuomorph.mu.MuF
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.{ProtobufF, Protocol}
import org.log4s._
import qq.droste.data.Mu
import qq.droste.data.Mu._

import scala.util.matching.Regex

object ProtoSrcGenerator extends SrcGenerator {

  private[this] val logger = getLogger

  val idlType: String = proto.IdlType

  def inputFiles(files: Set[File]): Seq[File] =
    files.filter(_.getName.endsWith(ProtoExtension)).toSeq

  def generateFrom(
      inputFile: File,
      serializationType: String,
      options: String*): Option[(String, Seq[String])] =
    getCode[IO](inputFile).map(Some(_)).unsafeRunSync

  def withImports(self: String): String =
    (self.split("\n", 2).toList match {
      case h :: t => imports(h) :: t
      case a      => a
    }).mkString("\n")

  val copRegExp: Regex = """((Cop\[)(((\w+)(\s)?(\:\:)(\s)?)+)(TNil)(\]))""".r

  val cleanCop: String => String =
    _.replace("Cop[", "").replace("::", ":+:").replace("TNil]", "CNil")

  val withCoproducts: String => String = self =>
    copRegExp.replaceAllIn(self, m => cleanCop(m.matched))

  val parseProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =
    higherkindness.skeuomorph.mu.Protocol.fromProtobufProto

  val printProtocol: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => String =
    higherkindness.skeuomorph.mu.print.proto.print

  private def getCode[F[_]: Sync](file: File): F[(String, Seq[String])] =
    parseProto[F, Mu[ProtobufF]]
      .parse(ProtoSource(file.getName, file.getParent))
      .map(
        protocol =>
          getPath(protocol) -> Seq(
            (parseProtocol andThen printProtocol andThen withImports andThen withCoproducts)(
              protocol)))

  private def getPath(p: Protocol[Mu[ProtobufF]]): String =
    s"${p.pkg.replace('.', '/')}/${p.name}$ScalaFileExtension"

  def imports(pkg: String): String =
    s"$pkg\nimport higherkindness.mu.rpc.protocol._\nimport fs2.Stream\nimport shapeless.{:+:, CNil}"
}
