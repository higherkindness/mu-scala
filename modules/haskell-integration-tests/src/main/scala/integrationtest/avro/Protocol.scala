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

package integrationtest.avro

import higherkindness.mu.rpc.protocol._

object weather {

  final case class GetForecastRequest(
      city: String,
      days_required: Int
  )

  final case class GetForecastResponse(
      last_updated: String,
      daily_forecasts: List[GetForecastResponse.Weather.Value]
  )

  object GetForecastResponse {

    object Weather extends Enumeration {
      type Weather = Value
      val SUNNY, CLOUDY, RAINY = Value
    }

  }

  @service(Avro, Gzip, namespace = Some("integrationtest"))
  trait WeatherService[F[_]] {

    def ping(req: Empty.type): F[Empty.type]

    def getForecast(req: GetForecastRequest): F[GetForecastResponse]

  }

}
