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

import higherkindness.mu.rpc.protocol._
import pbdirect._
import enumeratum.values.{IntEnum, IntEnumEntry}
import fs2.Stream

object weather {

  final case class GetForecastRequest(
      @pbIndex(1) city: String,
      @pbIndex(5) days_required: Int
  )

  final case class GetForecastResponse(
      @pbIndex(1) last_updated: String,
      // Note: Mu-Haskell does not understand packed repeated fields
      // (https://github.com/higherkindness/mu-haskell/issues/163)
      @pbIndex(2) @pbUnpacked daily_forecasts: List[GetForecastResponse.Weather]
  )

  object GetForecastResponse {

    sealed abstract class Weather(val value: Int) extends IntEnumEntry
    object Weather extends IntEnum[Weather] {
      case object SUNNY  extends Weather(0)
      case object CLOUDY extends Weather(1)
      case object RAINY  extends Weather(2)

      val values = findValues
    }

  }

  final case class RainEvent(
      @pbIndex(1) city: String,
      // Note: Mu-Haskell does not understand packed repeated fields
      // (https://github.com/higherkindness/mu-haskell/issues/163)
      @pbIndex(2) event_type: RainEvent.EventType
  )

  object RainEvent {

    sealed abstract class EventType(val value: Int) extends IntEnumEntry
    object EventType extends IntEnum[EventType] {
      case object STARTED extends EventType(0)
      case object STOPPED extends EventType(1)

      val values = findValues
    }

  }

  final case class RainSummaryResponse(
      @pbIndex(1) rained_count: Int
  )

  final case class SubscribeToRainEventsRequest(
      @pbIndex(1) city: String
  )

  @service(Protobuf, Gzip, namespace = Some("integrationtest"))
  trait WeatherService[F[_]] {

    def ping(req: Empty.type): F[Empty.type]

    def getForecast(req: GetForecastRequest): F[GetForecastResponse]

    def publishRainEvents(req: Stream[F, RainEvent]): F[RainSummaryResponse]

    def subscribeToRainEvents(req: SubscribeToRainEventsRequest): Stream[F, RainEvent]

  }

}
