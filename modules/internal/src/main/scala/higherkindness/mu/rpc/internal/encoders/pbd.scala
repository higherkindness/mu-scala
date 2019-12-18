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

package higherkindness.mu.rpc
package internal.encoders

import java.io.{ByteArrayInputStream, InputStream}
import java.time.{Instant, LocalDate, LocalDateTime}

import cats.instances._
import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import higherkindness.mu.rpc.internal.util.{BigDecimalUtil, EncoderUtil, JavaTimeUtil}
import io.grpc.MethodDescriptor.Marshaller
import org.apache.commons.compress.utils.IOUtils

object pbd extends OptionInstances with ListInstances {

  import pbdirect._

  implicit def defaultDirectPBMarshallers[A: PBMessageWriter: PBReader]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = IOUtils.toByteArray(stream).pbTo[A]

      override def stream(value: A): InputStream = new ByteArrayInputStream(value.toPB)

    }

  object bigDecimal {

    implicit object BigDecimalWriter extends PBFieldWriter[BigDecimal] {
      override def writeTo(index: Int, value: BigDecimal, out: CodedOutputStream): Unit =
        out.writeByteArray(index, BigDecimalUtil.bigDecimalToByte(value))
    }

    implicit object BigDecimalReader extends PBReader[BigDecimal] {
      override def read(input: CodedInputStream): BigDecimal =
        BigDecimalUtil.byteToBigDecimal(input.readByteArray())
    }
  }

  object javatime {

    implicit object LocalDateWriter extends PBFieldWriter[LocalDate] {
      override def writeTo(index: Int, value: LocalDate, out: CodedOutputStream): Unit =
        out.writeByteArray(index, EncoderUtil.intToByteArray(JavaTimeUtil.localDateToInt(value)))
    }

    implicit object LocalDateReader extends PBReader[LocalDate] {
      override def read(input: CodedInputStream): LocalDate =
        JavaTimeUtil.intToLocalDate(EncoderUtil.byteArrayToInt(input.readByteArray()))
    }

    implicit object LocalDateTimeWriter extends PBFieldWriter[LocalDateTime] {
      override def writeTo(index: Int, value: LocalDateTime, out: CodedOutputStream): Unit =
        out.writeByteArray(
          index,
          EncoderUtil.longToByteArray(JavaTimeUtil.localDateTimeToLong(value)))
    }

    implicit object LocalDateTimeReader extends PBReader[LocalDateTime] {
      override def read(input: CodedInputStream): LocalDateTime =
        JavaTimeUtil.longToLocalDateTime(EncoderUtil.byteArrayToLong(input.readByteArray()))
    }

    implicit object InstantWriter extends PBFieldWriter[Instant] {
      override def writeTo(index: Int, value: Instant, out: CodedOutputStream): Unit =
        out.writeByteArray(index, EncoderUtil.longToByteArray(JavaTimeUtil.instantToLong(value)))
    }

    implicit object InstantReader extends PBReader[Instant] {
      override def read(input: CodedInputStream): Instant =
        JavaTimeUtil.longToInstant(EncoderUtil.byteArrayToLong(input.readByteArray()))
    }

  }
}
