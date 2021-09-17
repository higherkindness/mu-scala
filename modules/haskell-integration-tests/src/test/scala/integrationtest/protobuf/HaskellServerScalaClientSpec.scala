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

package integrationtest.protobuf

import cats.effect.{IO, Resource}
import fs2._
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.protocol.Empty
import integrationtest._
import integrationtest.protobuf.weather.GetForecastResponse.Weather.SUNNY
import integrationtest.protobuf.weather.RainEvent.EventType._
import integrationtest.protobuf.weather._
import io.grpc.CallOptions
import munit.CatsEffectSuite

import scala.concurrent.duration._

class HaskellServerScalaClientSpec extends CatsEffectSuite with HaskellServerRunningInDocker {

  def serverPort: Int              = Constants.ProtobufPort
  def serverExecutableName: String = "protobuf-server"

  val channelFor: ChannelFor = ChannelForAddress("localhost", serverPort)

  val clientResource: Resource[IO, WeatherService[IO]] = WeatherService.client[IO](
    channelFor,
    options = CallOptions.DEFAULT.withCompression("gzip")
  )

  val clientFixture = ResourceSuiteLocalFixture("rpc-client", clientResource)

  override def munitFixtures = List(clientFixture)

  val behaviorOf = "Mu-Haskell server and Mu-Scala client communication using Protobuf"

  test(behaviorOf + "it should work for a trivial unary call") {
    IO(clientFixture()).flatMap(_.ping(Empty)).assertEquals(Empty)
  }

  test(behaviorOf + "it should work for a unary call") {
    val request = GetForecastRequest("London", 3)
    val expectedResponse = GetForecastResponse(
      last_updated = "2020-03-20T12:00:00Z",
      daily_forecasts = List(SUNNY, SUNNY, SUNNY)
    )
    IO(clientFixture()).flatMap(_.getForecast(request)).assertEquals(expectedResponse)
  }

  test(behaviorOf + "it should work for a client-streaming call") {
    val stream =
      Stream(STARTED, STOPPED, STARTED, STOPPED, STARTED)
        .map(RainEvent("London", _))
        .covary[IO]
    val expectedResponse = RainSummaryResponse(3)
    IO(clientFixture()).flatMap(_.publishRainEvents(stream)).assertEquals(expectedResponse)
  }

  test(behaviorOf + "it should work for a server-streaming call") {
    val request = SubscribeToRainEventsRequest("London")
    val expectedEvents =
      List(STARTED, STOPPED, STARTED, STOPPED, STARTED)
        .map(RainEvent("London", _))
    IO(clientFixture())
      .flatMap(client => Stream.force(client.subscribeToRainEvents(request)).compile.toList)
      .assertEquals(expectedEvents)
      .timeout(10.seconds)
  }

}
