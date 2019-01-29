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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalDateTime}

import com.google.common.io.ByteStreams
import higherkindness.mu.rpc.internal.util.{BigDecimalUtil, EncoderUtil, JavaTimeUtil}
import higherkindness.mu.rpc.protocol.Empty
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.{Conversions, LogicalTypes, Schema}
import org.apache.avro.Schema.Field
import shapeless.Nat
import shapeless.ops.nat.ToInt
import shapeless.tag.@@

import scala.math.BigDecimal.RoundingMode

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

  @deprecated(
    "This avro4s encoder doesn't fulfill the avro specs. " +
      "Be aware that this serializes without taking into account the precision and scale, " +
      "and it could lead to serialization problems if the client uses a different serializer.",
    "0.15.1"
  )
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

  object bigDecimalTagged {

    private[this] def toDecimalTag[SP](bigDecimal: BigDecimal): BigDecimal @@ SP =
      shapeless.tag[SP][BigDecimal](bigDecimal)

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode) =
      ScaleAndPrecisionAndRoundingMode(Nat.toInt[B], Nat.toInt[A], RM)

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode) =
      ScaleAndPrecisionAndRoundingMode(Nat.toInt[C], Nat.toInt[A] * 10 + Nat.toInt[B], RM)

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode) =
      ScaleAndPrecisionAndRoundingMode(
        Nat.toInt[C] * 10 + Nat.toInt[D],
        Nat.toInt[A] * 10 + Nat.toInt[B],
        RM)

    private[this] case class BDSerializer(sp: ScaleAndPrecisionAndRoundingMode) {

      val decimalConversion                 = new Conversions.DecimalConversion
      val decimalType: LogicalTypes.Decimal = LogicalTypes.decimal(sp.precision, sp.scale)

      def toByteBuffer(value: BigDecimal): ByteBuffer = {
        val scaledValue = value.setScale(sp.scale, sp.roundingMode)
        decimalConversion.toBytes(scaledValue.bigDecimal, null, decimalType)
      }

      def fromByteBuffer(value: ByteBuffer): BigDecimal =
        decimalConversion.fromBytes(value, null, decimalType)
    }

    private[this] def bigDecimalToSchema[A, B](
        sp: ScaleAndPrecisionAndRoundingMode): ToSchema[BigDecimal @@ (A, B)] = {
      new ToSchema[BigDecimal @@ (A, B)] {
        protected val schema = {
          val schema = Schema.create(Schema.Type.BYTES)
          LogicalTypes.decimal(sp.precision, sp.scale).addToSchema(schema)
          schema
        }
      }
    }

    private[this] def bigDecimalFromValue[A, B](
        sp: ScaleAndPrecisionAndRoundingMode): FromValue[BigDecimal @@ (A, B)] = {
      new FromValue[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp)
        override def apply(value: Any, field: Field): BigDecimal @@ (A, B) =
          toDecimalTag[(A, B)](inner.fromByteBuffer(value.asInstanceOf[ByteBuffer]))
      }
    }

    private[this] def bigDecimalToValue[A, B](
        sp: ScaleAndPrecisionAndRoundingMode): ToValue[BigDecimal @@ (A, B)] = {
      new ToValue[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp)
        override def apply(value: BigDecimal @@ (A, B)): ByteBuffer =
          inner.toByteBuffer(value)
      }
    }

    implicit def bigDecimalToSchemaSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToSchema[BigDecimal @@ (A, B)] =
      bigDecimalToSchema[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalToSchemaBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToSchema[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalToSchema[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalToSchemaBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToSchema[
      BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalToSchema[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    implicit def bigDecimalFromValueSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): FromValue[BigDecimal @@ (A, B)] =
      bigDecimalFromValue[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalFromValueBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): FromValue[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalFromValue[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalFromValueBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): FromValue[
      BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalFromValue[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    implicit def bigDecimalToValueSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToValue[BigDecimal @@ (A, B)] =
      bigDecimalToValue[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalToValueBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToValue[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalToValue[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalToValueBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): ToValue[
      BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalToValue[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    object marshallers {

      implicit def bigDecimalMarshaller2[A <: Nat, B <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Marshaller[
        BigDecimal @@ (A, B)] =
        new Marshaller[BigDecimal @@ (A, B)] {
          val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B])

          override def stream(value: BigDecimal @@ (A, B)): InputStream =
            new ByteArrayInputStream(inner.toByteBuffer(value).array())

          override def parse(stream: InputStream): BigDecimal @@ (A, B) =
            toDecimalTag[(A, B)](
              inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream))))
        }

      implicit def bigDecimalMarshaller3[A <: Nat, B <: Nat, C <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          toIntNC: ToInt[C],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Marshaller[
        BigDecimal @@ ((A, B), C)] = new Marshaller[BigDecimal @@ ((A, B), C)] {
        val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B, C])

        override def stream(value: BigDecimal @@ ((A, B), C)): InputStream =
          new ByteArrayInputStream(inner.toByteBuffer(value).array())

        override def parse(stream: InputStream): BigDecimal @@ ((A, B), C) =
          toDecimalTag[((A, B), C)](
            inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream))))
      }

      implicit def bigDecimalMarshaller4[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          toIntNC: ToInt[C],
          toIntND: ToInt[D],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Marshaller[
        BigDecimal @@ ((A, B), (C, D))] = new Marshaller[BigDecimal @@ ((A, B), (C, D))] {
        val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B, C, D])

        override def stream(value: BigDecimal @@ ((A, B), (C, D))): InputStream =
          new ByteArrayInputStream(inner.toByteBuffer(value).array())

        override def parse(stream: InputStream): BigDecimal @@ ((A, B), (C, D)) =
          toDecimalTag[((A, B), (C, D))](
            inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream))))
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
