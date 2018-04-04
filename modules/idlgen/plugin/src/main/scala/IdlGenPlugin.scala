/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc.idlgen

import java.io.File
import sbt.Keys._
import sbt._

object IdlGenPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val idlGen: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates IDL files from freestyle-rpc service definitions")

    lazy val srcGen: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates freestyle-rpc Scala files from IDL definitions")

    lazy val idlType: SettingKey[String] =
      settingKey[String]("The IDL type to work with, such as avro or proto")

    lazy val idlGenSourceDir: SettingKey[File] =
      settingKey[File](
        "The Scala source directory, where your freestyle-rpc service definitions are placed.")

    lazy val idlGenTargetDir: SettingKey[File] =
      settingKey[File](
        "The IDL target directory, where the `idlGen` task will write the generated files " +
          "in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on freestyle-rpc service definitions.")

    lazy val srcGenSourceDir: SettingKey[File] =
      settingKey[File]("The IDL directory, where your IDL definitions are placed.")

    lazy val srcGenTargetDir: SettingKey[File] =
      settingKey[File](
        "The Scala target directory, where the `srcGen` task will write the generated files " +
          "in subpackages based on the namespaces declared in the IDL files.")

    lazy val genOptions: SettingKey[Seq[String]] =
      settingKey[Seq[String]](
        "Options for the generator, such as additional @rpc annotation parameters in srcGen.")
  }

  import freestyle.rpc.idlgen.IdlGenPlugin.autoImport._

  lazy val defaultSettings: Seq[Def.Setting[_]] = Seq(
    idlType := "(missing arg)",
    idlGenSourceDir := (Compile / sourceDirectory).value,
    idlGenTargetDir := (Compile / resourceManaged).value,
    srcGenSourceDir := (Compile / resourceDirectory).value,
    srcGenTargetDir := (Compile / sourceManaged).value,
    genOptions := Seq.empty
  )

  lazy val taskSettings: Seq[Def.Setting[_]] = {
    Seq(
      idlGen := idlGenTask(
        IdlGenApplication,
        idlType.value,
        genOptions.value,
        idlGenTargetDir.value,
        target.value / "idlGen")(idlGenSourceDir.value.allPaths.get.toSet).toSeq,
      srcGen := idlGenTask(
        SrcGenApplication,
        idlType.value,
        genOptions.value,
        srcGenTargetDir.value,
        target.value / "srcGen")(srcGenSourceDir.value.allPaths.get.toSet).toSeq
    )
  }

  private def idlGenTask(
      generator: GeneratorApplication[_],
      idlType: String,
      options: Seq[String],
      targetDir: File,
      cacheDir: File): Set[File] => Set[File] =
    FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.exists) {
      (inputFiles: Set[File]) =>
        generator.generateFrom(idlType, inputFiles, targetDir, options: _*).toSet
    }

  override def projectSettings: Seq[Def.Setting[_]] =
    defaultSettings ++ taskSettings ++ Seq(
      libraryDependencies += "io.frees" %% "frees-rpc-idlgen-core" % freestyle.rpc.idlgen.BuildInfo.version
    )
}
