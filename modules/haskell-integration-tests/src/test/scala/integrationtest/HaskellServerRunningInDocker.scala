/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package integrationtest

import com.whisk.docker.DockerContainer
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.impl.spotify.DockerKitSpotify
import org.scalatest.Suite

trait HaskellServerRunningInDocker extends DockerTestKit with DockerKitSpotify { self: Suite =>

  def serverPort: Int
  def serverExecutableName: String

  override def dockerContainers: List[DockerContainer] =
    List(
      DockerContainer(Constants.ImageName)
        .withPorts(serverPort -> Some(serverPort))
        .withCommand(s"/opt/mu-haskell-client-server/$serverExecutableName")
    )

  override def startAllOrFail(): Unit = {
    println("Starting Mu-Haskell server in Docker container...")
    super.startAllOrFail()
    println("Started Docker container.")
    Thread.sleep(2000) // give the Haskell server a chance to start up properly
  }

}
