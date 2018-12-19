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
package internal.encoders

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalDateTime}

import com.google.common.io.ByteStreams
import higherkindness.mu.rpc.internal.util.{BigDecimalUtil, EncoderUtil}
import higherkindness.mu.rpc.protocol.Empty
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.{Conversions, LogicalTypes, Schema, SchemaBuilder}
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

  implicit def avroMarshallers[A: SchemaFor: Decoder: Encoder]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = {
        val input: AvroInputStream[A] =
          AvroInputStream.binary[A].from(stream).build(SchemaFor[A].schema)
        val result = input.iterator.next()
        input.close()
        result
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
        val output: AvroOutputStream[A] =
          AvroOutputStream.binary[A].to(baos).build(SchemaFor[A].schema)
        output.write(value)
        output.flush()
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

    implicit object bigDecimalToSchema extends SchemaFor[BigDecimal] {
      override val schema: Schema = SchemaBuilder.builder.bytesType()
    }

    implicit object bigDecimalFromValue extends Decoder[BigDecimal] {
      override def decode(value: Any, schema: Schema): BigDecimal =
        BigDecimalUtil.byteToBigDecimal(value.asInstanceOf[ByteBuffer].array())
    }

    implicit object bigDecimalToValue extends Encoder[BigDecimal] {
      override def encode(value: BigDecimal, schema: Schema): AnyRef =
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
      ScalePrecisionRoundingMode(Nat.toInt[B], Nat.toInt[A], RM)

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode) =
      ScalePrecisionRoundingMode(Nat.toInt[C], Nat.toInt[A] * 10 + Nat.toInt[B], RM)

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode) =
      ScalePrecisionRoundingMode(
        Nat.toInt[C] * 10 + Nat.toInt[D],
        Nat.toInt[A] * 10 + Nat.toInt[B],
        RM)

    private[this] case class BDSerializer(sp: ScalePrecisionRoundingMode) {

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
        sp: ScalePrecisionRoundingMode): SchemaFor[BigDecimal @@ (A, B)] = {
      new SchemaFor[BigDecimal @@ (A, B)] {
        val schema: Schema =
          LogicalTypes.decimal(sp.precision, sp.scale).addToSchema(SchemaBuilder.builder.bytesType)
      }
    }

    private[this] def bigDecimalFromValue[A, B](
        sp: ScalePrecisionRoundingMode): Decoder[BigDecimal @@ (A, B)] = {
      new Decoder[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp)
        override def decode(value: Any, schema: Schema): @@[BigDecimal, (A, B)] =
          toDecimalTag[(A, B)](inner.fromByteBuffer(value.asInstanceOf[ByteBuffer]))
      }
    }

    private[this] def bigDecimalToValue[A, B](
        sp: ScalePrecisionRoundingMode): Encoder[BigDecimal @@ (A, B)] = {
      new Encoder[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp)
        override def encode(value: @@[BigDecimal, (A, B)], schema: Schema): AnyRef =
          inner.toByteBuffer(value)
      }
    }

    implicit def bigDecimalToSchemaSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): SchemaFor[BigDecimal @@ (A, B)] =
      bigDecimalToSchema[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalToSchemaBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): SchemaFor[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalToSchema[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalToSchemaBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): SchemaFor[
      BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalToSchema[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    implicit def bigDecimalFromValueSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Decoder[BigDecimal @@ (A, B)] =
      bigDecimalFromValue[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalFromValueBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Decoder[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalFromValue[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalFromValueBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Decoder[
      BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalFromValue[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    implicit def bigDecimalToValueSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Encoder[BigDecimal @@ (A, B)] =
      bigDecimalToValue[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalToValueBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Encoder[
      BigDecimal @@ ((A, B), C)] =
      bigDecimalToValue[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalToValueBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY): Encoder[
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

    object marshallers {

      implicit def localDateMarshaller(
          implicit E: Encoder[LocalDate],
          D: Decoder[LocalDate]): Marshaller[LocalDate] = new Marshaller[LocalDate] {

        val schema: Schema = SchemaBuilder.builder().intType()

        override def stream(value: LocalDate): InputStream =
          new ByteArrayInputStream(
            EncoderUtil.intToByteArray(E.encode(value, schema).asInstanceOf[Int]))

        override def parse(stream: InputStream): LocalDate =
          D.decode(EncoderUtil.byteArrayToInt(ByteStreams.toByteArray(stream)), schema)

      }

      implicit def localDateTimeMarshaller(
          implicit E: Encoder[LocalDateTime],
          D: Decoder[LocalDateTime]): Marshaller[LocalDateTime] =
        new Marshaller[LocalDateTime] {

          val schema: Schema = SchemaBuilder.builder().longType()

          override def stream(value: LocalDateTime): InputStream =
            new ByteArrayInputStream(
              EncoderUtil.longToByteArray(E.encode(value, schema).asInstanceOf[Long]))

          override def parse(stream: InputStream): LocalDateTime =
            D.decode(EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)), schema)
        }

      implicit def instantMarshaller(
          implicit E: Encoder[Instant],
          D: Decoder[Instant]): Marshaller[Instant] =
        new Marshaller[Instant] {

          val schema: Schema = SchemaBuilder.builder().longType()

          override def stream(value: Instant): InputStream =
            new ByteArrayInputStream(
              EncoderUtil.longToByteArray(E.encode(value, schema).asInstanceOf[Long]))

          override def parse(stream: InputStream): Instant =
            D.decode(EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream)), schema)
        }
    }

  }
}

object avrowithschema extends AvroMarshallers {

  import com.sksamuel.avro4s._

  implicit def avroWithSchemaMarshallers[A <: Product: Encoder: Decoder: SchemaFor]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = {
        val input: AvroInputStream[A] =
          AvroInputStream.data[A].from(stream).build(SchemaFor[A].schema)
        val result = input.iterator.next()
        input.close()
        result
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
        val output: AvroOutputStream[A] =
          AvroOutputStream.data[A].to(baos).build(SchemaFor[A].schema)
        output.write(value)
        output.flush()
        output.close()

        new ByteArrayInputStream(baos.toByteArray)
      }

    }
}
