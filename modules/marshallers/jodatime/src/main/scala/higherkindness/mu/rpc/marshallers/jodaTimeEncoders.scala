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

package higherkindness.mu.rpc
package marshallers

import java.io.{ByteArrayInputStream, InputStream}

import cats.syntax.contravariant._
import cats.syntax.functor._
import com.google.common.io.ByteStreams
import higherkindness.mu.rpc.internal.util.EncoderUtil
import higherkindness.mu.rpc.jodatime.util.JodaTimeUtil
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.Schema
import org.apache.avro.Schema.Field
import org.joda.time.{LocalDate, LocalDateTime}

object jodaTimeEncoders {

  object pbd {

    import pbdirect._

    implicit val jodaLocalDateWriter: PBScalarValueWriter[LocalDate] =
      PBScalarValueWriter[Int].contramap[LocalDate](JodaTimeUtil.jodaLocalDateToInt)

    implicit val jodaLocalDateReader: PBScalarValueReader[LocalDate] =
      PBScalarValueReader[Int].map(JodaTimeUtil.intToJodaLocalDate)

    implicit val jodaLocalDateTimeWriter: PBScalarValueWriter[LocalDateTime] =
      PBScalarValueWriter[Long].contramap[LocalDateTime](JodaTimeUtil.jodaLocalDateTimeToLong)

    implicit val jodaLocalDateTimeReader: PBScalarValueReader[LocalDateTime] =
      PBScalarValueReader[Long].map(JodaTimeUtil.longToJodaLocalDateTime)

  }

  object avro {

    import com.sksamuel.avro4s._

    implicit object JodaLocalDateToSchema extends ToSchema[LocalDate] {
      override val schema: Schema = Schema.create(Schema.Type.INT)
    }

    implicit object JodaLocalDateFromValue extends FromValue[LocalDate] {
      override def apply(value: Any, field: Field): LocalDate =
        JodaTimeUtil.intToJodaLocalDate(value.asInstanceOf[Int])
    }

    implicit object JodalocalDateToValue extends ToValue[LocalDate] {
      override def apply(value: LocalDate): Int =
        JodaTimeUtil.jodaLocalDateToInt(value)
    }

    implicit object JodaLocalDateTimeToSchema extends ToSchema[LocalDateTime] {
      override val schema: Schema = Schema.create(Schema.Type.LONG)
    }

    implicit object JodaLocalDateTimeFromValue extends FromValue[LocalDateTime] {
      def apply(value: Any, field: Field): LocalDateTime =
        JodaTimeUtil.longToJodaLocalDateTime(value.asInstanceOf[Long])
    }

    implicit object JodaLocalDateTimeToValue extends ToValue[LocalDateTime] {
      override def apply(value: LocalDateTime): Long =
        JodaTimeUtil.jodaLocalDateTimeToLong(value)
    }

    /*
     * These marshallers are only used when the entire gRPC request/response
     * is a LocalDate/LocalDateTime. When a LocalDate/LocalDateTime is a field
     * of a larger message, the polymorphic marshaller defined in
     * higherkindness.mu.rpc.internal.encoders.{avro,avrowithschema} is used.
     */
    object marshallers {

      implicit object JodaLocalDateMarshaller extends Marshaller[LocalDate] {
        override def stream(value: LocalDate): InputStream =
          new ByteArrayInputStream(
            EncoderUtil.intToByteArray(JodaTimeUtil.jodaLocalDateToInt(value))
          )

        override def parse(stream: InputStream): LocalDate =
          JodaTimeUtil.intToJodaLocalDate(
            EncoderUtil.byteArrayToInt(ByteStreams.toByteArray(stream))
          )
      }

      implicit object JodaLocalDateTimeMarshaller extends Marshaller[LocalDateTime] {
        override def stream(value: LocalDateTime): InputStream =
          new ByteArrayInputStream(
            EncoderUtil.longToByteArray(JodaTimeUtil.jodaLocalDateTimeToLong(value))
          )

        override def parse(stream: InputStream): LocalDateTime =
          JodaTimeUtil.longToJodaLocalDateTime(
            EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream))
          )
      }

    }
  }

}
