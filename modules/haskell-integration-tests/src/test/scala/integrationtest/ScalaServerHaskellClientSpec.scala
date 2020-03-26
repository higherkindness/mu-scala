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

package integrationtest

import weather._
import higherkindness.mu.rpc.server.{AddService, GrpcServer}

import com.spotify.docker.client._
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.DockerClient._

import cats.effect.{ContextShift, IO}

import org.scalatest.Suite
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext
import org.scalatest.BeforeAndAfterAll

trait DockerClientStuff { self: Suite =>

  val docker = DefaultDockerClient.fromEnv().build()

  val hostExternalIpAddress = "192.168.0.89" // TODO

  def containerConfig(clientArgs: List[String]) =
    ContainerConfig
      .builder()
      .image("mu-haskell-protobuf:latest")
      .cmd(("/opt/mu-haskell-protobuf/client" :: hostExternalIpAddress :: clientArgs): _*)
      .build()

  def runHaskellClient(clientArgs: List[String]) = {
    val containerCreation = docker.createContainer(containerConfig(clientArgs))
    val id                = containerCreation.id()
    docker.startContainer(id)
    val exit = docker.waitContainer(id)
    assert(exit.statusCode() == 0)
    val logstream = docker.logs(id, LogsParam.stdout(), LogsParam.stderr())
    try {
      logstream.readFully().trim()
    } finally {
      logstream.close()
    }
  }

}

class ScalaServerHaskellClientSpec
    extends AnyFlatSpec
    with DockerClientStuff
    with BeforeAndAfterAll {

  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit val service: WeatherService[IO] = new MyWeatherService[IO]

  private val startServer: IO[Unit] = for {
    serviceDef <- WeatherService.bindService[IO]
    serverDef  <- GrpcServer.default[IO](9123, List(AddService(serviceDef)))
    _          <- GrpcServer.server[IO](serverDef)
  } yield ()

  private var cancelToken: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    cancelToken = startServer.unsafeRunCancelable {
      case Left(e) =>
        println(s"Server failed! $e")
      case Right(_) =>
        println("Server completed (this should never happen)")
    }
    Thread.sleep(500) // give the server a chance to start up
  }

  override def afterAll(): Unit =
    // stop the server
    cancelToken.unsafeRunSync()

  behavior of "Mu-Scala server and Mu-Haskell client communication using Protobuf"

  it should "work for a trivial unary call" in {
    val clientOutput = runHaskellClient(List("ping"))
    assert(clientOutput == "pong")
  }

  it should "work for a unary call" in {
    val clientOutput = runHaskellClient(List("get-forecast", "London", "3"))
    assert(clientOutput == "TODO")
  }

}
