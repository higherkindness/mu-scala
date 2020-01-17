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

package higherkindness.mu.rpc.srcgen.openapi

import java.io.File
import java.nio.file.{Path, Paths}
import higherkindness.mu.rpc.srcgen._
import higherkindness.skeuomorph.openapi._
import schema.OpenApi
import ParseOpenApi._
import print._
import client.print._
import client.http4s.circe._
import client.http4s.print._

import cats.data.Nested
import cats.implicits._

import higherkindness.skeuomorph.Parser
import cats.effect._
import higherkindness.skeuomorph.openapi.JsonSchemaF
import scala.collection.JavaConverters._

object OpenApiSrcGenerator {
  sealed trait HttpImpl extends Product with Serializable
  object HttpImpl {
    case object Http4sV20 extends HttpImpl
    case object Http4sV18 extends HttpImpl
  }

  def apply(httpImpl: HttpImpl, resourcesBasePath: Path): SrcGenerator = new SrcGenerator {
    def idlType: String = IdlType

    implicit def http4sSpecifics: Http4sSpecifics = httpImpl match {
      case HttpImpl.Http4sV18 => client.http4s.print.v18.v18Http4sSpecifics
      case HttpImpl.Http4sV20 => client.http4s.print.v20.v20Http4sSpecifics
    }
    protected def inputFiles(files: Set[File]): Seq[File] =
      files.filter(handleFile(_)(_ => true, _ => true, false)).toSeq

    protected def generateFrom(
        inputFile: File,
        serializationType: String,
        options: String*): Option[(String, Seq[String])] =
      getCode[IO](inputFile).value.unsafeRunSync()

    private def getCode[F[_]: Sync](file: File): Nested[F, Option, (String, Seq[String])] =
      parseFile[F]
        .apply(file)
        .map(OpenApi.extractNestedTypes[JsonSchemaF.Fixed])
        .map { openApi =>
          val (_, paths) =
            file.getParentFile.toPath.asScala
              .splitAt(resourcesBasePath.iterator().asScala.size + 1) //we need to add one because it is changing the resource path, adding open api
          val path: Path = Paths.get(paths.map(_.toString()).mkString("/"))
          val pkg        = packageName(path)
          pathFrom(path, file).toString ->
            Seq(
              s"package ${pkg.value}",
              model[JsonSchemaF.Fixed].print(openApi),
              interfaceDefinition.print(openApi),
              impl.print(pkg -> openApi)
            ).filter(_.nonEmpty)
        }

    private def packageName(path: Path): PackageName =
      PackageName(path.iterator.asScala.map(_.toString).mkString("."))

    private def pathFrom(path: Path, file: File): Path =
      path.resolve(s"${file.getName.split('.').head}$ScalaFileExtension")

    private def parseFile[F[_]: Sync]: File => Nested[F, Option, OpenApi[JsonSchemaF.Fixed]] =
      x =>
        Nested(
          handleFile(x)(
            Parser[F, JsonSource, OpenApi[JsonSchemaF.Fixed]].parse(_).map(_.some),
            Parser[F, YamlSource, OpenApi[JsonSchemaF.Fixed]].parse(_).map(_.some),
            Sync[F].delay(none)
          ))

    private def handleFile[T](
        file: File)(json: JsonSource => T, yaml: YamlSource => T, none: T): T = file match {
      case x if (x.getName().endsWith(JsonExtension)) => json(JsonSource(file))
      case x if (x.getName().endsWith(YamlExtension)) => yaml(YamlSource(file))
      case _                                          => none
    }
  }
}
