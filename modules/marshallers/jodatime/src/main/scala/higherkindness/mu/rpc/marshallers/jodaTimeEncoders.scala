/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

import com.google.common.io.ByteStreams
import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import com.sksamuel.avro4s.Decoder.{IntDecoder, LongDecoder}
import com.sksamuel.avro4s.Encoder.{IntEncoder, LongEncoder}
import higherkindness.mu.rpc.internal.util.EncoderUtil
import higherkindness.mu.rpc.jodatime.util.JodaTimeUtil
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.{Schema, SchemaBuilder}
import org.joda.time.{LocalDate, LocalDateTime}

object jodaTimeEncoders {

  object pbd {

    import pbdirect._

    implicit object JodaLocalDateWriter extends PBWriter[LocalDate] {
      override def writeTo(index: Int, value: LocalDate, out: CodedOutputStream): Unit =
        out.writeByteArray(
          index,
          EncoderUtil.intToByteArray(JodaTimeUtil.jodaLocalDateToInt(value)))
    }

    implicit object JodaLocalDateReader extends PBReader[LocalDate] {
      override def read(input: CodedInputStream): LocalDate =
        JodaTimeUtil.intToJodaLocalDate(EncoderUtil.byteArrayToInt(input.readByteArray()))
    }

    implicit object JodaLocalDateTimeWriter extends PBWriter[LocalDateTime] {
      override def writeTo(index: Int, value: LocalDateTime, out: CodedOutputStream): Unit =
        out.writeByteArray(
          index,
          EncoderUtil.longToByteArray(JodaTimeUtil.jodaLocalDatetimeToLong(value)))
    }

    implicit object JodaLocalDateTimeReader extends PBReader[LocalDateTime] {
      override def read(input: CodedInputStream): LocalDateTime =
        JodaTimeUtil.longToJodaLocalDateTime(EncoderUtil.byteArrayToLong(input.readByteArray()))
    }

  }

  object avro {

    import com.sksamuel.avro4s._

    implicit object JodaLocalDateToSchema extends SchemaFor[LocalDate] {
      override val schema: Schema = SchemaBuilder.builder().intType()
    }

    implicit val jodaLocalDateFromValue: Decoder[LocalDate] =
      IntDecoder.map(JodaTimeUtil.intToJodaLocalDate)

    implicit val jodalocalDateToValue: Encoder[LocalDate] =
      IntEncoder.comap[LocalDate](JodaTimeUtil.jodaLocalDateToInt)

    implicit object JodaLocalDateTimeToSchema extends SchemaFor[LocalDateTime] {
      override val schema: Schema = SchemaBuilder.builder().longType()
    }

    implicit val jodaLocalDateTimeFromValue: Decoder[LocalDateTime] =
      LongDecoder.map(JodaTimeUtil.longToJodaLocalDateTime)

    implicit val jodaLocalDateTimeToValue: Encoder[LocalDateTime] =
      LongEncoder.comap[LocalDateTime](JodaTimeUtil.jodaLocalDatetimeToLong)

    object marshallers {

      implicit object JodaLocalDateMarshaller extends Marshaller[LocalDate] {
        override def stream(value: LocalDate): InputStream =
          new ByteArrayInputStream(
            EncoderUtil.intToByteArray(JodaTimeUtil.jodaLocalDateToInt(value)))

        override def parse(stream: InputStream): LocalDate =
          JodaTimeUtil.intToJodaLocalDate(
            EncoderUtil.byteArrayToInt(ByteStreams.toByteArray(stream)))
      }

      implicit object JodaLocalDateTimeMarshaller extends Marshaller[LocalDateTime] {
        override def stream(value: LocalDateTime): InputStream =
          new ByteArrayInputStream(
            EncoderUtil.longToByteArray(JodaTimeUtil.jodaLocalDatetimeToLong(value)))

        override def parse(stream: InputStream): LocalDateTime =
          JodaTimeUtil.longToJodaLocalDateTime(
            EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)))
      }

    }
  }

}
