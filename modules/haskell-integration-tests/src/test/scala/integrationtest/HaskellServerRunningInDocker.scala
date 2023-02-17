/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

import com.spotify.docker.client.messages.PortBinding
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.testkit.{
  ContainerCommandExecutor,
  ContainerSpec,
  DockerContainerManager,
  DockerTestTimeouts,
  ManagedContainers
}

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

trait HaskellServerRunningInDocker { self: munit.FunSuite =>

  def serverPort: Int
  def serverExecutableName: String

  val dockerClient: DockerClient = DefaultDockerClient.fromEnv().build()

  val dockerExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())

  val managedContainers: ManagedContainers = ContainerSpec(Constants.ImageName)
    .withPortBindings(serverPort -> PortBinding.of("0.0.0.0", serverPort))
    .withCommand(s"/opt/mu-haskell-client-server/$serverExecutableName")
    .toContainer
    .toManagedContainer

  val dockerTestTimeouts: DockerTestTimeouts = DockerTestTimeouts.Default

  implicit lazy val dockerExecutor: ContainerCommandExecutor =
    new ContainerCommandExecutor(dockerClient)

  lazy val containerManager = new DockerContainerManager(
    managedContainers,
    dockerExecutor,
    dockerTestTimeouts,
    dockerExecutionContext
  )

  override def beforeAll(): Unit = {
    println("Starting Mu-Haskell server in Docker container...")
    containerManager.start()
    println("Started Docker container.")
    Thread.sleep(2000) // give the Haskell server a chance to start up properly
  }

  override def afterAll(): Unit =
    containerManager.stop()

}
