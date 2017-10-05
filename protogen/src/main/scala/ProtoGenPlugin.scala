/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle

import sbt._
import sbt.Keys._

object ProtoGenPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val protoGen: TaskKey[Unit] =
      taskKey[Unit]("Generates .proto files from freestyle-rpc service definitions")

    lazy val protoGenSourceDir: SettingKey[File] =
      settingKey[File](
        "The Scala source directory, where your freestyle-rpc service definitions are placed.")

    lazy val protoGenTargetDir: SettingKey[File] =
      settingKey[File](
        "The Protocol Buffers target directory, where the `protoGen` task will " +
          "write the `.proto` files, based on freestyle-rpc service definitions.")
  }

  import autoImport._

  lazy val defaultSettings: Seq[Def.Setting[_]] = Seq(
    protoGenSourceDir := baseDirectory.value / "src" / "main" / "scala",
    protoGenTargetDir := baseDirectory.value / "src" / "main" / "proto"
  )

  lazy val protoGenTaskSettings: Seq[Def.Setting[_]] = Seq(
    protoGen := {
      (runner in Compile).value
        .run(
          mainClass = "freestyle.rpc.protocol.ProtoCodeGen",
          classpath = sbt.Attributed.data((fullClasspath in Compile).value),
          options = Seq(
            protoGenSourceDir.value.absolutePath,
            protoGenTargetDir.value.absolutePath
          ),
          log = streams.value.log
        )
      (): Unit
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings ++ protoGenTaskSettings
}
