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

import cats.effect.IO
import cats.effect.kernel.Resource
import com.spotify.docker.client._
import com.spotify.docker.client.messages.{ContainerConfig, ContainerExit}
import com.spotify.docker.client.DockerClient._

trait RunHaskellClientInDocker {

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

  def runHaskellClientR(clientArgs: List[String]): IO[String] = {

    def acquire: IO[(LogStream, ContainerExit)] = IO {
      val containerCreation = docker.createContainer(containerConfig(clientArgs))
      val id                = containerCreation.id()
      docker.startContainer(id)
      val exit = docker.waitContainer(id)
      val logs = docker.logs(id, LogsParam.stdout(), LogsParam.stderr())
      (logs, exit)
    }

    Resource.make(acquire)(t => IO(t._1.close())).use { case (logs, exit) =>
      IO(logs.readFully().trim()).flatTap { output =>
        if (exit.statusCode != 0)
          IO.raiseError[String](
            new java.lang.AssertionError(
              s"Client exited with code ${exit.statusCode} and output: $output"
            )
          )
        else IO.unit
      }
    }
  }

}
