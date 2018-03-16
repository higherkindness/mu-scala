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

import sbt._
import sbt.Keys._

object IdlGenPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val idlGen: TaskKey[Unit] =
      taskKey[Unit]("Generates IDL files from freestyle-rpc service definitions")

    lazy val sourceDir: SettingKey[File] =
      settingKey[File](
        "The Scala source directory, where your freestyle-rpc service definitions are placed.")

    lazy val targetDir: SettingKey[File] =
      settingKey[File](
        "The IDL target directory, where the `idlGen` task will write the generated files " +
          "in subdirectories such as `proto` for Protobuf and `avro` for Avro, based on freestyle-rpc service definitions.")
  }

  import autoImport._

  lazy val defaultSettings: Seq[Def.Setting[_]] = Seq(
    sourceDir := baseDirectory.value / "src" / "main" / "scala",
    targetDir := baseDirectory.value / "src" / "main" / "resources"
  )

  lazy val taskSettings: Seq[Def.Setting[_]] = Seq(
    idlGen := {
      (runner in Compile).value
        .run(
          mainClass = "freestyle.rpc.idlgen.Application",
          classpath = sbt.Attributed.data((fullClasspath in Compile).value),
          options = Seq(
            sourceDir.value.absolutePath,
            targetDir.value.absolutePath
          ),
          log = streams.value.log
        )
      (): Unit
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    defaultSettings ++ taskSettings ++ Seq(
      libraryDependencies += "io.frees" %% "frees-rpc-idlgen-core" % freestyle.rpc.idlgen.BuildInfo.version
    )
}
