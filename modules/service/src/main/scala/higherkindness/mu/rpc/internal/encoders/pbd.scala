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

package higherkindness.mu.rpc
package internal.encoders

import java.io.{ByteArrayInputStream, InputStream}
import java.time.{Instant, LocalDate, LocalDateTime}

import cats.instances._
import cats.syntax.contravariant._
import cats.syntax.functor._
import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import higherkindness.mu.rpc.internal.util.{BigDecimalUtil, JavaTimeUtil}
import io.grpc.MethodDescriptor.Marshaller
import org.apache.commons.compress.utils.IOUtils
import com.google.protobuf.WireFormat

object pbd extends OptionInstances with ListInstances {

  import pbdirect._

  implicit def defaultDirectPBMarshallers[A: PBMessageWriter: PBMessageReader]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = IOUtils.toByteArray(stream).pbTo[A]

      override def stream(value: A): InputStream = new ByteArrayInputStream(value.toPB)

    }

  object bigDecimal {

    implicit object BigDecimalWriter extends PBScalarValueWriter[BigDecimal] {
      override def wireType: Int                         = WireFormat.WIRETYPE_LENGTH_DELIMITED
      override def isDefault(value: BigDecimal): Boolean = false
      override def writeWithoutTag(value: BigDecimal, out: CodedOutputStream): Unit =
        out.writeByteArrayNoTag(BigDecimalUtil.bigDecimalToByte(value))
    }

    implicit object BigDecimalReader extends PBScalarValueReader[BigDecimal] {
      override def defaultValue: BigDecimal = BigDecimal(0.0)
      override def canBePacked: Boolean     = false
      override def read(input: CodedInputStream): BigDecimal =
        BigDecimalUtil.byteToBigDecimal(input.readByteArray())
    }
  }

  object javatime {

    implicit val localDateWriter: PBScalarValueWriter[LocalDate] =
      PBScalarValueWriter[Int].contramap[LocalDate](JavaTimeUtil.localDateToInt)

    implicit val localDateReader: PBScalarValueReader[LocalDate] =
      PBScalarValueReader[Int].map(JavaTimeUtil.intToLocalDate)

    implicit val localDateTimeWriter: PBScalarValueWriter[LocalDateTime] =
      PBScalarValueWriter[Long].contramap[LocalDateTime](JavaTimeUtil.localDateTimeToLong)

    implicit val localDateTimeReader: PBScalarValueReader[LocalDateTime] =
      PBScalarValueReader[Long].map(JavaTimeUtil.longToLocalDateTime)

    implicit val instantWriter: PBScalarValueWriter[Instant] =
      PBScalarValueWriter[Long].contramap[Instant](JavaTimeUtil.instantToLong)

    implicit val instantReader: PBScalarValueReader[Instant] =
      PBScalarValueReader[Long].map(JavaTimeUtil.longToInstant)

  }

}
