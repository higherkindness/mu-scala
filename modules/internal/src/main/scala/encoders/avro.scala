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

package freestyle.rpc
package internal.encoders

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalDateTime}

import com.google.common.io.ByteStreams
import freestyle.rpc.internal.util.{BigDecimalUtil, EncoderUtil, JavaTimeUtil}
import freestyle.rpc.protocol.Empty
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.Schema
import org.apache.avro.Schema.Field

trait AvroMarshallers {

  implicit val emptyMarshallers: Marshaller[Empty.type] = new Marshaller[Empty.type] {
    override def parse(stream: InputStream): Empty.type = Empty
    override def stream(value: Empty.type)              = new ByteArrayInputStream(Array.empty)
  }

}

object avro extends AvroMarshallers {

  import com.sksamuel.avro4s._

  implicit def avroMarshallers[A: SchemaFor: FromRecord: ToRecord]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = {
        val bytes: Array[Byte] =
          Iterator.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
        val in: ByteArrayInputStream        = new ByteArrayInputStream(bytes)
        val input: AvroBinaryInputStream[A] = AvroInputStream.binary[A](in)
        input.iterator.toList.head
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream       = new ByteArrayOutputStream()
        val output: AvroBinaryOutputStream[A] = AvroOutputStream.binary[A](baos)
        output.write(value)
        output.close()

        new ByteArrayInputStream(baos.toByteArray)
      }

    }

  object bigdecimal {

    implicit object bigDecimalToSchema extends ToSchema[BigDecimal] {
      override val schema: Schema = Schema.create(Schema.Type.BYTES)
    }

    implicit object bigDecimalFromValue extends FromValue[BigDecimal] {
      def apply(value: Any, field: Field): BigDecimal =
        BigDecimalUtil.byteToBigDecimal(value.asInstanceOf[ByteBuffer].array())
    }

    implicit object bigDecimalToValue extends ToValue[BigDecimal] {
      override def apply(value: BigDecimal): ByteBuffer =
        ByteBuffer.wrap(BigDecimalUtil.bigDecimalToByte(value))
    }

    object marshallers {

      implicit val bigDecimalMarshaller: Marshaller[BigDecimal] = new Marshaller[BigDecimal] {
        override def stream(value: BigDecimal): InputStream =
          new ByteArrayInputStream(BigDecimalUtil.bigDecimalToByte(value))

        override def parse(stream: InputStream): BigDecimal =
          BigDecimalUtil.byteToBigDecimal(ByteStreams.toByteArray(stream))
      }
    }
  }

  object javatime {

    implicit object localDateToSchema extends ToSchema[LocalDate] {
      override val schema: Schema = Schema.create(Schema.Type.INT)
    }

    implicit object localDateFromValue extends FromValue[LocalDate] {
      def apply(value: Any, field: Field): LocalDate =
        JavaTimeUtil.intToLocalDate(value.asInstanceOf[Int])
    }

    implicit object localDateToValue extends ToValue[LocalDate] {
      override def apply(value: LocalDate): Int =
        JavaTimeUtil.localDateToInt(value)
    }

    implicit object localDateTimeToSchema extends ToSchema[LocalDateTime] {
      override val schema: Schema = Schema.create(Schema.Type.LONG)
    }

    implicit object localDateTimeFromValue extends FromValue[LocalDateTime] {
      def apply(value: Any, field: Field): LocalDateTime =
        JavaTimeUtil.longToLocalDateTime(value.asInstanceOf[Long])
    }

    implicit object localDateTimeToValue extends ToValue[LocalDateTime] {
      override def apply(value: LocalDateTime): Long =
        JavaTimeUtil.localDateTimeToLong(value)
    }

    implicit object instantToSchema extends ToSchema[Instant] {
      override val schema: Schema = Schema.create(Schema.Type.LONG)
    }

    implicit object instantFromValue extends FromValue[Instant] {
      def apply(value: Any, field: Field): Instant =
        JavaTimeUtil.longToInstant(value.asInstanceOf[Long])
    }

    implicit object instantToValue extends ToValue[Instant] {
      override def apply(value: Instant): Long =
        JavaTimeUtil.instantToLong(value)
    }

    object marshallers {

      implicit val localDateMarshaller: Marshaller[LocalDate] = new Marshaller[LocalDate] {
        override def stream(value: LocalDate): InputStream =
          new ByteArrayInputStream(EncoderUtil.intToByteArray(JavaTimeUtil.localDateToInt(value)))

        override def parse(stream: InputStream): LocalDate =
          JavaTimeUtil.intToLocalDate(EncoderUtil.byteArrayToInt(ByteStreams.toByteArray(stream)))

      }

      implicit val localDateTimeMarshaller: Marshaller[LocalDateTime] =
        new Marshaller[LocalDateTime] {
          override def stream(value: LocalDateTime): InputStream =
            new ByteArrayInputStream(
              EncoderUtil.longToByteArray(JavaTimeUtil.localDateTimeToLong(value)))

          override def parse(stream: InputStream): LocalDateTime =
            JavaTimeUtil.longToLocalDateTime(
              EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)))
        }

      implicit val instantMarshaller: Marshaller[Instant] =
        new Marshaller[Instant] {
          override def stream(value: Instant): InputStream =
            new ByteArrayInputStream(EncoderUtil.longToByteArray(JavaTimeUtil.instantToLong(value)))

          override def parse(stream: InputStream): Instant =
            JavaTimeUtil.longToInstant(EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)))
        }
    }

  }
}

object avrowithschema extends AvroMarshallers {

  import com.sksamuel.avro4s._
  import org.apache.avro.file.DataFileStream
  import org.apache.avro.generic.{GenericDatumReader, GenericRecord}

  implicit def avroWithSchemaMarshallers[A: ToRecord](
      implicit schemaFor: SchemaFor[A],
      fromRecord: FromRecord[A]): Marshaller[A] = new Marshaller[A] {

    override def parse(stream: InputStream): A = {
      val dfs = new DataFileStream(stream, new GenericDatumReader[GenericRecord](schemaFor()))
      fromRecord(dfs.next())
    }

    override def stream(value: A): InputStream = {
      val baos: ByteArrayOutputStream     = new ByteArrayOutputStream()
      val output: AvroDataOutputStream[A] = AvroOutputStream.data[A](baos)
      output.write(value)
      output.close()

      new ByteArrayInputStream(baos.toByteArray)
    }

  }
}
