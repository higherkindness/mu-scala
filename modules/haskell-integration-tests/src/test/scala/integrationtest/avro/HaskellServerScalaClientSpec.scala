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

package integrationtest.avro

import integrationtest._
import weather._
import weather.GetForecastResponse.Weather.SUNNY
import higherkindness.mu.rpc._
import higherkindness.mu.rpc.protocol.Empty

import io.grpc.CallOptions
import cats.effect.{ContextShift, IO, Resource}

import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext

class HaskellServerScalaClientSpec extends AnyFlatSpec with HaskellServerRunningInDocker {

  def serverPort: Int              = Constants.AvroPort
  def serverExecutableName: String = "avro-server"

  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val channelFor: ChannelFor = ChannelForAddress("localhost", serverPort)

  val clientResource: Resource[IO, WeatherService[IO]] = WeatherService.client[IO](
    channelFor,
    options = CallOptions.DEFAULT.withCompression("gzip")
  )

  behavior of "Mu-Haskell server and Mu-Scala client communication using Avro"

  it should "work for a trivial unary call" in {
    pending
    // until we upgrade to a version of Mu-Haskell that contains this fix:
    // https://github.com/haskell-grpc-native/http2-grpc-haskell/pull/20
    //
    // However, I think this test may be failing for another reason.  I'm not
    // sure what the correct behaviour is for a 'void' Avro method in gRPC.
    // Mu-Scala treats that as "the server returns an `Empty` message", but I
    // don't know what the Mu-Haskell server is returning. The test fails with
    // "io.grpc.StatusRuntimeException: INTERNAL: No value received for unary
    // call".

    val response = clientResource
      .use(client => client.ping(Empty))
      .unsafeRunSync()
    assert(response == Empty)
  }

  it should "work for a unary call" in {
    pending
    // until we upgrade to a version of Mu-Haskell that contains this fix:
    // https://github.com/haskell-grpc-native/http2-grpc-haskell/pull/20

    val request = GetForecastRequest("London", 3)
    val expectedResponse = GetForecastResponse(
      last_updated = "2020-03-20T12:00:00Z",
      daily_forecasts = List(SUNNY, SUNNY, SUNNY)
    )
    val response = clientResource
      .use(client => client.getForecast(request))
      .unsafeRunSync()
    assert(response == expectedResponse)
  }

}
