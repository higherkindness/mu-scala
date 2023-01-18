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

package integrationtest.avro

import cats.effect.{IO, Resource}
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import integrationtest._
import integrationtest.avro.weather._
import munit.CatsEffectSuite

class ScalaServerHaskellClientSpec extends CatsEffectSuite with RunHaskellClientInDocker {

  def clientExecutableName: String = "avro-client"

  implicit val service: WeatherService[IO] = new MyWeatherService[IO]

  private val startServer: Resource[IO, Unit] = for {
    serviceDef <- WeatherService.bindService[IO]
    _          <- GrpcServer.defaultServer[IO](Constants.AvroPort, List(AddService(serviceDef)))
  } yield ()

  val serverFixture = ResourceSuiteLocalFixture("rpc-server", startServer)

  override def munitFixtures = List(serverFixture)

  val behaviorOf = "Mu-Scala server and Mu-Haskell client communication using Avro"

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

}
