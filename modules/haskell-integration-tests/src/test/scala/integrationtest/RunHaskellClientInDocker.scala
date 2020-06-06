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

import com.spotify.docker.client._
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.DockerClient._

import org.scalatest.Suite

trait RunHaskellClientInDocker { self: Suite =>

  def clientExecutableName: String

  val docker = DefaultDockerClient.fromEnv().build()

  // An address for the RPC server that can be reached from inside a docker container
  val hostExternalIpAddress = {
    import sys.process._
    if (sys.props("os.name").contains("Mac"))
      "ipconfig getifaddr en0".!!.trim
    else
      "hostname -I".!!.trim.split(" ").head
  }

  def containerConfig(clientArgs: List[String]) =
    ContainerConfig
      .builder()
      .image(Constants.ImageName)
      .cmd(
        (s"/opt/mu-haskell-client-server/$clientExecutableName" :: hostExternalIpAddress :: clientArgs): _*
      )
      .build()

  def runHaskellClient(clientArgs: List[String]) = {
    val containerCreation = docker.createContainer(containerConfig(clientArgs))
    val id                = containerCreation.id()
    docker.startContainer(id)
    val exit      = docker.waitContainer(id)
    val logstream = docker.logs(id, LogsParam.stdout(), LogsParam.stderr())
    try {
      val output = logstream.readFully().trim()
      assert(
        exit.statusCode == 0,
        s"Client exited with code ${exit.statusCode} and output: $output"
      )
      output
    } finally logstream.close()
  }

}
