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

package higherkindness.mu.rpc.srcgen.proto

import java.io.File

import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import higherkindness.mu.rpc.srcgen.Model.{
  CompressionTypeGen,
  GzipGen,
  NoCompressionGen,
  StreamingImplementation,
  UseIdiomaticEndpoints
}
import higherkindness.mu.rpc.srcgen._
import higherkindness.skeuomorph.mu.{CompressionType, MuF}
import higherkindness.skeuomorph.protobuf.ParseProto.{parseProto, ProtoSource}
import higherkindness.skeuomorph.protobuf.{ProtobufF, Protocol}
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._

import higherkindness.mu.rpc.srcgen.Model.Fs2Stream
import higherkindness.mu.rpc.srcgen.Model.MonixObservable

import scala.meta._
import scala.util.control.NoStackTrace

object ProtoSrcGenerator {

  case class ProtobufSrcGenException(message: String) extends NoStackTrace

  def build(
      compressionTypeGen: CompressionTypeGen,
      useIdiomaticEndpoints: UseIdiomaticEndpoints,
      streamingImplementation: StreamingImplementation,
      idlTargetDir: File
  ): SrcGenerator = new SrcGenerator {

    val idlType: String = proto.IdlType

    def inputFiles(files: Set[File]): Seq[File] =
      files.filter(_.getName.endsWith(ProtoExtension)).toSeq

    def generateFrom(
        inputFile: File,
        serializationType: String
    ): Option[(String, Seq[String])] =
      getCode[IO](inputFile).map(Some(_)).unsafeRunSync

    val streamCtor: (Type, Type) => Type.Apply = streamingImplementation match {
      case Fs2Stream       => { case (f, a) => t"_root_.fs2.Stream[$f, $a]" }
      case MonixObservable => { case (_, a) => t"_root_.monix.reactive.Observable[$a]" }
    }

    val skeuomorphCompression: CompressionType = compressionTypeGen match {
      case GzipGen          => CompressionType.Gzip
      case NoCompressionGen => CompressionType.Identity
    }

    val transformToMuProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[
      MuF
    ]] =
      higherkindness.skeuomorph.mu.Protocol
        .fromProtobufProto(skeuomorphCompression, useIdiomaticEndpoints)

    val generateScalaSource: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => Either[
      String,
      String
    ] =
      higherkindness.skeuomorph.mu.codegen.protocol(_, streamCtor).map(_.syntax)

    val splitLines: String => List[String] = _.split("\n").toList

    private def getCode[F[_]](file: File)(implicit F: Sync[F]): F[(String, Seq[String])] =
      parseProto[F, Mu[ProtobufF]]
        .parse(ProtoSource(file.getName, file.getParent, Some(idlTargetDir.getCanonicalPath)))
        .flatMap { protocol =>
          val path = getPath(protocol)
          (transformToMuProtocol andThen generateScalaSource)(protocol) match {
            case Left(error) =>
              F.raiseError(
                ProtobufSrcGenException(
                  s"Failed to generate Scala source from Protobuf file ${file.getAbsolutePath}. Error details: $error"
                )
              )
            case Right(fileContent) =>
              F.pure(path -> splitLines(fileContent))
          }
        }

    private def getPath(p: Protocol[Mu[ProtobufF]]): String =
      s"${p.pkg.replace('.', '/')}/${p.name}$ScalaFileExtension"

  }
}
