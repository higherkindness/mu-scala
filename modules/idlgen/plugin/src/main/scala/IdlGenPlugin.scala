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
import sbt.io.{Path, PathFinder}

object IdlGenPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val idlGen: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates IDL files from freestyle-rpc service definitions")

    lazy val srcGen: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("Generates freestyle-rpc Scala files from IDL definitions")

    val srcGenFromJars =
      taskKey[Seq[File]]("Unzip IDL definitions from the given jar files")

    lazy val idlType: SettingKey[String] =
      settingKey[String]("The IDL type to work with, such as avro or proto")

    lazy val idlExtension: SettingKey[String] =
      settingKey[String](
        "The IDL extension to work with, files with a different extension will be omitted. Bu default 'avdl' for avro and 'proto' for proto")

    lazy val srcGenSerializationType: SettingKey[String] =
      settingKey[String](
        "The serialization type when generating Scala sources from the IDL definitions." +
          "Protobuf, Avro or AvroWithSchema are the current supported serialization types. " +
          "By default, the serialization type is 'Avro'.")

    lazy val idlGenSourceDir: SettingKey[File] =
      settingKey[File](
        "The Scala source directory, where your freestyle-rpc service definitions are placed.")

    lazy val idlGenTargetDir: SettingKey[File] =
      settingKey[File](
        "The IDL target directory, where the `idlGen` task will write the generated files " +
          "in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on freestyle-rpc service definitions.")

    lazy val srcGenSourceFromJarsDir: SettingKey[File] =
      settingKey[File](
        "The list of directories where your IDL files are placed")

    @deprecated("This settings is deprecated in favor of srcGenSourceDirs", "0.14.0")
    lazy val srcGenSourceDir: SettingKey[File] =
      settingKey[File]("The IDL directory, where your IDL definitions are placed.")

    lazy val srcGenSourceDirs: SettingKey[Seq[File]] =
      settingKey[Seq[File]]("The IDL directories, where your IDL definitions are placed.")

    lazy val srcJarNames: SettingKey[Seq[String]] =
      settingKey[Seq[String]](
        "The names of those jars containing IDL definitions that will be used at " +
          "compilation time to generate the Scala Sources. By default, this sequence is empty.")

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
    srcGenSerializationType := "Avro",
    idlExtension := (if (idlType.value == "avro") "avdl"
                     else if (idlType.value == "proto") "proto"
                     else "unknown"),
    idlGenSourceDir := (Compile / sourceDirectory).value,
    idlGenTargetDir := (Compile / resourceManaged).value,
    srcGenSourceFromJarsDir := (Compile / resourceManaged).value / idlType.value,
    srcGenSourceDir := (Compile / resourceDirectory).value,
    srcGenSourceDirs := Seq(srcGenSourceDir.value, srcGenSourceFromJarsDir.value),
    srcJarNames := Seq.empty,
    srcGenTargetDir := (Compile / sourceManaged).value,
    genOptions := Seq.empty
  )

  lazy val taskSettings: Seq[Def.Setting[_]] = {
    Seq(
      idlGen := idlGenTask(
        IdlGenApplication,
        idlType.value,
        srcGenSerializationType.value,
        genOptions.value,
        idlGenTargetDir.value,
        target.value / "idlGen")(idlGenSourceDir.value.allPaths.get.toSet).toSeq,
      srcGen := idlGenTask(
        SrcGenApplication,
        idlType.value,
        srcGenSerializationType.value,
        genOptions.value,
        srcGenTargetDir.value,
        target.value / "srcGen")(srcGenSourceDirs.value.allPaths.get.toSet).toSeq,
      srcGenFromJars := {
        Def
          .sequential(
            Def.task {
              (dependencyClasspath in Compile).value.map(
                entry =>
                  extractIDLDefinitionsFromJar(
                    entry,
                    srcJarNames.value,
                    srcGenSourceFromJarsDir.value,
                    idlExtension.value))
            },
            srcGen
          )
          .value
      }
    )
  }

  private def idlGenTask(
      generator: GeneratorApplication[_],
      idlType: String,
      serializationType: String,
      options: Seq[String],
      targetDir: File,
      cacheDir: File): Set[File] => Set[File] =
    FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.exists) {
      (inputFiles: Set[File]) =>
        generator.generateFrom(idlType, serializationType, inputFiles, targetDir, options: _*).toSet
    }

  private def extractIDLDefinitionsFromJar(
      classpathEntry: Attributed[File],
      jarNames: Seq[String],
      target: File,
      idlExtension: String): File = {

    val nameFilter: NameFilter = new NameFilter {
      override def accept(name: String): Boolean =
        name.toLowerCase.endsWith("." + idlExtension)
    }

    classpathEntry.get(artifact.key).fold((): Unit) { entryArtifact =>
      if (jarNames.exists(entryArtifact.name.startsWith)) {
        IO.withTemporaryDirectory { tmpDir =>
          if (classpathEntry.data.isDirectory) {
            val sources = PathFinder(classpathEntry.data).allPaths pair Path
              .rebase(classpathEntry.data, target)
            IO.copy(
              sources.filter(tuple => nameFilter.accept(tuple._2)),
              overwrite = true,
              preserveLastModified = true,
              preserveExecutable = true)
          } else {
            IO.unzip(classpathEntry.data, tmpDir, nameFilter)
            IO.copyDirectory(tmpDir, target)
          }
        }
      }
    }
    target
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    defaultSettings ++ taskSettings ++ Seq(
      libraryDependencies += "io.frees" %% "frees-rpc-idlgen-core" % freestyle.rpc.idlgen.BuildInfo.version
    )
}
