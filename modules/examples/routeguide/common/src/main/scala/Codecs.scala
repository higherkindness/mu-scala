/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package example.routeguide.common

import io.circe._
import io.circe.generic.semiauto._
import example.routeguide.protocol.Protocols._

object Codecs {

  implicit val pointDecoder: Decoder[Point] = deriveDecoder[Point]
  implicit val pointEncoder: Encoder[Point] = deriveEncoder[Point]

  implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder[Feature]

  implicit val featureDBDecoder: Decoder[FeatureDatabase] = deriveDecoder[FeatureDatabase]
  implicit val featureDBEncoder: Encoder[FeatureDatabase] = deriveEncoder[FeatureDatabase]

}
