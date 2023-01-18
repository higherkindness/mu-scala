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

package higherkindness.mu.rpc.avro

import org.apache.avro.Schema
import com.sksamuel.avro4s._
import scala.compiletime.ops.int._

object Decimals:

  opaque type TaggedDecimal[P <: Int, S <: Int] = BigDecimal

  extension [P <: Int, S <: Int](td: TaggedDecimal[P, S]) def value: BigDecimal = td

  object TaggedDecimal:

    def apply[P <: Int, S <: Int](bd: BigDecimal)(using
        p: ValueOf[P],
        s: ValueOf[S],
        scaleNotGreaterThanPrecision: S <= P =:= true
    ): Either[String, TaggedDecimal[P, S]] =
      if (bd.precision > p.value) {
        Left(
          s"Precision is too high. Maximum allowed precision is ${p.value}, but this BigDecimal has precision ${bd.precision}."
        )
      } else if ((s.value - bd.scale) > (p.value - bd.precision)) {
        Left(
          s"Scale (${bd.scale}) is too low. Calling setScale(${s.value}) would cause precision to become ${bd.precision + (s.value - bd.scale)}, which exceeds the maximum precision (${p.value})."
        )
      } else {
        Right(bd)
      }

    given [P <: Int, S <: Int](using p: ValueOf[P], s: ValueOf[S]): SchemaFor[TaggedDecimal[P, S]]
      with
      def schema: Schema =
        val sp = ScalePrecision(s.value, p.value)
        new BigDecimalSchemaFor(sp).schema

    given [P <: Int, S <: Int]: Encoder[TaggedDecimal[P, S]] =
      Encoder.given_Encoder_BigDecimal

    given [P <: Int, S <: Int]: Decoder[TaggedDecimal[P, S]] =
      Decoder.given_Decoder_BigDecimal
