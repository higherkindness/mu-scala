/*
 * Copyright 2017-2022 47 Degrees Open Source <https://www.47deg.com>
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

package integrationtest.protobuf

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import integrationtest._
import integrationtest.protobuf.weather._
import munit.CatsEffectSuite

import scala.concurrent.duration._

class ScalaServerHaskellClientSpec extends CatsEffectSuite with RunHaskellClientInDocker {

  def clientExecutableName: String = "protobuf-client"

  implicit val service: WeatherService[IO] = new MyWeatherService[IO]

  private val startServer: Resource[IO, Unit] = for {
    serviceDef <- WeatherService.bindService[IO]
    _          <- GrpcServer.defaultServer[IO](Constants.ProtobufPort, List(AddService(serviceDef)))
  } yield ()

  val serverFixture = ResourceSuiteLocalFixture("rpc-server", startServer)

  override def munitFixtures = List(serverFixture)

  val behaviorOf = "Mu-Scala server and Mu-Haskell client communication using Protobuf"

  test(behaviorOf + "it should work for a trivial unary call") {
    IO(serverFixture()) *>
      runHaskellClientR(List("ping")).assertEquals("pong")
  }

  test(behaviorOf + "it should work for a unary call") {
    IO(serverFixture()) *>
      runHaskellClientR(List("get-forecast", "London", "3")).assertEquals(
        """2020-03-20T12:00:00Z ["SUNNY","SUNNY","SUNNY"]"""
      )
  }

  test(behaviorOf + "it should work for a client-streaming call") {
    IO(serverFixture()) *>
      runHaskellClientR(List("publish-rain-events", "London")).assertEquals(
        "It started raining 3 times"
      )
  }

  test(behaviorOf + "it should work for a server-streaming call") {
    IO(serverFixture()) *>
      runHaskellClientR(List("subscribe-to-rain-events", "London"))
        .assertEquals(
          """|"STARTED"
         |"STOPPED"
         |"STARTED"
         |"STOPPED"
         |"STARTED"""".stripMargin
        )
        .timeout(10.seconds)
  }

}
