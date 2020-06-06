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

import cats.Applicative
import cats.syntax.applicative._
import weather._
import higherkindness.mu.rpc.protocol.Empty

class MyWeatherService[F[_]: Applicative] extends WeatherService[F] {

  def ping(req: Empty.type): F[Empty.type] =
    Empty.pure[F]

  def getForecast(req: weather.GetForecastRequest): F[GetForecastResponse] = {
    val lastUpdated    = "2020-03-20T12:00:00Z"
    val days           = if (req.days_required <= 0) 5 else req.days_required
    val dailyForecasts = List.fill(days)(GetForecastResponse.Weather.SUNNY)
    GetForecastResponse(lastUpdated, dailyForecasts).pure[F]
  }

}
