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

package higherkindness.mu.rpc.idlgen.proto

import java.io.File

import cats.effect.{IO, Sync}
import cats.syntax.functor._
import cats.syntax.option._
import higherkindness.mu.rpc.idlgen.Model.{
  CompressionTypeGen,
  GzipGen,
  NoCompressionGen,
  StreamingImplementation,
  UseIdiomaticEndpoints
}
import higherkindness.mu.rpc.idlgen._
import higherkindness.skeuomorph.mu.{CompressionType, MuF}
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.{ProtobufF, Protocol}
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._

import higherkindness.mu.rpc.idlgen.Model.Fs2Stream
import higherkindness.mu.rpc.idlgen.Model.MonixObservable

object ProtoSrcGenerator {

  def build(
      compressionTypeGen: CompressionTypeGen,
      useIdiomaticEndpoints: UseIdiomaticEndpoints,
      streamingImplementation: StreamingImplementation,
      idlTargetDir: File): SrcGenerator = new SrcGenerator {

    val idlType: String = proto.IdlType

    def inputFiles(files: Set[File]): Seq[File] =
      files.filter(_.getName.endsWith(ProtoExtension)).toSeq

    def generateFrom(
        inputFile: File,
        serializationType: String,
        options: String*): Option[(String, Seq[String])] =
      getCode[IO](inputFile).map(Some(_)).unsafeRunSync

    def withImports(lines: List[String]): List[String] =
      lines match {
        case h :: t =>
          // first line of file is package declaration
          h :: imports(t) ++ t
        case a => a
      }

    val streamPattern = "Stream[F, "

    def withStreamingImpl(lines: List[String]): List[String] = streamingImplementation match {
      case Fs2Stream =>
        lines.map(_.replaceAllLiterally(streamPattern, "_root_.fs2.Stream[F, "))
      case MonixObservable =>
        lines.map(_.replaceAllLiterally(streamPattern, "_root_.monix.reactive.Observable["))
    }

    val skeuomorphCompression: CompressionType = compressionTypeGen match {
      case GzipGen          => CompressionType.Gzip
      case NoCompressionGen => CompressionType.Identity
    }

    val parseProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =
      higherkindness.skeuomorph.mu.Protocol
        .fromProtobufProto(skeuomorphCompression, useIdiomaticEndpoints)

    val printProtocol: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => String =
      higherkindness.skeuomorph.mu.print.proto.print

    val splitLines: String => List[String] = _.split("\n").toList

    private def getCode[F[_]: Sync](file: File): F[(String, Seq[String])] =
      parseProto[F, Mu[ProtobufF]]
        .parse(ProtoSource(file.getName, file.getParent, Some(idlTargetDir.getCanonicalPath)))
        .map(protocol =>
          getPath(protocol) ->
            (parseProtocol andThen printProtocol andThen splitLines andThen withStreamingImpl andThen withImports)(
              protocol))

    private def getPath(p: Protocol[Mu[ProtobufF]]): String =
      s"${p.pkg.replace('.', '/')}/${p.name}$ScalaFileExtension"

    def imports(fileLines: List[String]): List[String] = {
      List(
        "import higherkindness.mu.rpc.protocol._".some,
        // TODO the shapeless imports can be removed when we upgrade skeuomorph
        // (see https://github.com/higherkindness/skeuomorph/pull/197)
        if (fileLines.exists(_.contains(":+:")))
          "import shapeless.{:+:, CNil}".some
        else
          None
      ).flatten
    }

  }
}
