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
package internal.encoders

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalDateTime}

import com.google.common.io.ByteStreams
import higherkindness.mu.rpc.internal.util.{BigDecimalUtil, EncoderUtil, JavaTimeUtil}
import higherkindness.mu.rpc.protocol.Empty
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.{Conversions, LogicalTypes, Schema}
import org.apache.avro.LogicalTypes.{TimestampMicros, TimestampMillis}
import com.sksamuel.avro4s.SchemaFor.TimestampNanosLogicalType
import shapeless.Nat
import shapeless.ops.nat.ToInt
import shapeless.tag.@@

import scala.math.BigDecimal.RoundingMode
import java.time.ZoneOffset

object avro {

  implicit val emptyMarshaller: Marshaller[Empty.type] = new Marshaller[Empty.type] {
    override def parse(stream: InputStream): Empty.type = Empty
    override def stream(value: Empty.type)              = new ByteArrayInputStream(Array.empty)
  }

  import com.sksamuel.avro4s._

  /*
   * Marshaller for Avro record types.
   * Warning: it's possible to call this for non-record types (e.g. Int),
   * but it will blow up at runtime.
   */
  implicit def avroMarshallers[A: SchemaFor: Encoder: Decoder]: Marshaller[A] =
    new Marshaller[A] {

      override def parse(stream: InputStream): A = {
        val input: AvroInputStream[A] = AvroInputStream.binary[A].from(stream).build(AvroSchema[A])
        input.iterator.toList.head
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
        val output: AvroOutputStream[A] = AvroOutputStream.binary[A].to(baos).build(AvroSchema[A])
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

    implicit object bigDecimalSchemaFor extends SchemaFor[BigDecimal] {
      def schema(fm: FieldMapper): Schema = Schema.create(Schema.Type.BYTES)
    }

    implicit object bigDecimalDecoder extends Decoder[BigDecimal] {
      def decode(value: Any, schema: Schema, fm: FieldMapper): BigDecimal =
        BigDecimalUtil.byteToBigDecimal(value.asInstanceOf[ByteBuffer].array())
    }

    implicit object bigDecimalEncoder extends Encoder[BigDecimal] {
      def encode(value: BigDecimal, schema: Schema, fm: FieldMapper): ByteBuffer =
        ByteBuffer.wrap(BigDecimalUtil.bigDecimalToByte(value))
    }

    /*
     * These marshallers are only used when the entire gRPC request/response is
     * a BigDecimal. When a BigDecimal is a field of a larger message, the
     * polymorphic marshaller defined in the `avro` or `avrowithschema` object
     * is used.
     */
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
        toIntNB: ToInt[B]
    ) =
      ScalePrecision(Nat.toInt[B], Nat.toInt[A])

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C]
    ) =
      ScalePrecision(Nat.toInt[C], Nat.toInt[A] * 10 + Nat.toInt[B])

    private[this] def avro4sPrecisionAndScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D]
    ) =
      ScalePrecision(
        Nat.toInt[C] * 10 + Nat.toInt[D],
        Nat.toInt[A] * 10 + Nat.toInt[B]
      )

    private[this] case class BDSerializer(sp: ScalePrecision, rm: RoundingMode.RoundingMode) {

      val decimalConversion                 = new Conversions.DecimalConversion
      val decimalType: LogicalTypes.Decimal = LogicalTypes.decimal(sp.precision, sp.scale)

      def toByteBuffer(value: BigDecimal): ByteBuffer = {
        val scaledValue = value.setScale(sp.scale, rm)
        decimalConversion.toBytes(scaledValue.bigDecimal, null, decimalType)
      }

      def fromByteBuffer(value: ByteBuffer): BigDecimal =
        decimalConversion.fromBytes(value, null, decimalType)
    }

    private[this] def bigDecimalSchemaFor[A, B](
        sp: ScalePrecision
    ): SchemaFor[BigDecimal @@ (A, B)] = {
      new SchemaFor[BigDecimal @@ (A, B)] {
        def schema(fm: FieldMapper) = {
          val schema = Schema.create(Schema.Type.BYTES)
          LogicalTypes.decimal(sp.precision, sp.scale).addToSchema(schema)
          schema
        }
      }
    }

    private[this] def bigDecimalDecoder[A, B](
        sp: ScalePrecision,
        rm: RoundingMode.RoundingMode
    ): Decoder[BigDecimal @@ (A, B)] = {
      new Decoder[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp, rm)
        def decode(value: Any, schema: Schema, fm: FieldMapper): BigDecimal @@ (A, B) =
          toDecimalTag[(A, B)](inner.fromByteBuffer(value.asInstanceOf[ByteBuffer]))
      }
    }

    private[this] def bigDecimalEncoder[A, B](
        sp: ScalePrecision,
        rm: RoundingMode.RoundingMode
    ): Encoder[BigDecimal @@ (A, B)] = {
      new Encoder[BigDecimal @@ (A, B)] {
        val inner = BDSerializer(sp, rm)
        def encode(value: BigDecimal @@ (A, B), schema: Schema, fm: FieldMapper): ByteBuffer =
          inner.toByteBuffer(value)
      }
    }

    implicit def bigDecimalSchemaForSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B]
    ): SchemaFor[BigDecimal @@ (A, B)] =
      bigDecimalSchemaFor[A, B](avro4sPrecisionAndScale[A, B])

    implicit def bigDecimalSchemaForBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C]
    ): SchemaFor[BigDecimal @@ ((A, B), C)] =
      bigDecimalSchemaFor[(A, B), C](avro4sPrecisionAndScale[A, B, C])

    implicit def bigDecimalSchemaForBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D]
    ): SchemaFor[BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalSchemaFor[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D])

    implicit def bigDecimalDecoderSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Decoder[BigDecimal @@ (A, B)] =
      bigDecimalDecoder[A, B](avro4sPrecisionAndScale[A, B], RM)

    implicit def bigDecimalDecoderBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Decoder[BigDecimal @@ ((A, B), C)] =
      bigDecimalDecoder[(A, B), C](avro4sPrecisionAndScale[A, B, C], RM)

    implicit def bigDecimalDecoderBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Decoder[BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalDecoder[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D], RM)

    implicit def bigDecimalEncoderSimple[A <: Nat, B <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Encoder[BigDecimal @@ (A, B)] =
      bigDecimalEncoder[A, B](avro4sPrecisionAndScale[A, B], RM)

    implicit def bigDecimalEncoderBigPrecision[A <: Nat, B <: Nat, C <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Encoder[BigDecimal @@ ((A, B), C)] =
      bigDecimalEncoder[(A, B), C](avro4sPrecisionAndScale[A, B, C], RM)

    implicit def bigDecimalEncoderBigPrecisionScale[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
        implicit toIntNA: ToInt[A],
        toIntNB: ToInt[B],
        toIntNC: ToInt[C],
        toIntND: ToInt[D],
        RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
    ): Encoder[BigDecimal @@ ((A, B), (C, D))] =
      bigDecimalEncoder[(A, B), (C, D)](avro4sPrecisionAndScale[A, B, C, D], RM)

    /*
     * These marshallers are only used when the entire gRPC request/response is
     * a tagged BigDecimal. When a tagged BigDecimal is a field of a larger
     * message, the polymorphic marshaller defined in the `avro` or
     * `avrowithschema` object is used.
     */
    object marshallers {

      implicit def bigDecimalMarshaller2[A <: Nat, B <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
      ): Marshaller[BigDecimal @@ (A, B)] =
        new Marshaller[BigDecimal @@ (A, B)] {
          val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B], RM)

          override def stream(value: BigDecimal @@ (A, B)): InputStream =
            new ByteArrayInputStream(inner.toByteBuffer(value).array())

          override def parse(stream: InputStream): BigDecimal @@ (A, B) =
            toDecimalTag[(A, B)](
              inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream)))
            )
        }

      implicit def bigDecimalMarshaller3[A <: Nat, B <: Nat, C <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          toIntNC: ToInt[C],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
      ): Marshaller[BigDecimal @@ ((A, B), C)] = new Marshaller[BigDecimal @@ ((A, B), C)] {
        val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B, C], RM)

        override def stream(value: BigDecimal @@ ((A, B), C)): InputStream =
          new ByteArrayInputStream(inner.toByteBuffer(value).array())

        override def parse(stream: InputStream): BigDecimal @@ ((A, B), C) =
          toDecimalTag[((A, B), C)](
            inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream)))
          )
      }

      implicit def bigDecimalMarshaller4[A <: Nat, B <: Nat, C <: Nat, D <: Nat](
          implicit toIntNA: ToInt[A],
          toIntNB: ToInt[B],
          toIntNC: ToInt[C],
          toIntND: ToInt[D],
          RM: RoundingMode.RoundingMode = RoundingMode.UNNECESSARY
      ): Marshaller[BigDecimal @@ ((A, B), (C, D))] =
        new Marshaller[BigDecimal @@ ((A, B), (C, D))] {
          val inner: BDSerializer = BDSerializer(avro4sPrecisionAndScale[A, B, C, D], RM)

          override def stream(value: BigDecimal @@ ((A, B), (C, D))): InputStream =
            new ByteArrayInputStream(inner.toByteBuffer(value).array())

          override def parse(stream: InputStream): BigDecimal @@ ((A, B), (C, D)) =
            toDecimalTag[((A, B), (C, D))](
              inner.fromByteBuffer(ByteBuffer.wrap(ByteStreams.toByteArray(stream)))
            )
        }
    }
  }

  object javatime {

    /* We used to define our own SchemaFor/Encoder/Decoders for
     * LocalDate and Instant, but Avro4s 3.x provides them out of the box
     * and they match the encoding we used, so they have been removed.
     *
     * Kept our custom codec for LocalDateTime for compatibility reasons,
     * because it is different from the built-in avro4s one (they encode
     * the datetime as nanoseconds).
     */

    implicit object localDateTimeSchemaFor extends SchemaFor[LocalDateTime] {
      override def schema(fm: FieldMapper): Schema = Schema.create(Schema.Type.LONG)
    }

    implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
      Decoder[Long].map(JavaTimeUtil.longToLocalDateTime)

    implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
      Encoder[Long].comap(JavaTimeUtil.localDateTimeToLong)

    /*
     * These marshallers are only used when the entire gRPC request/response
     * is a LocalDate/LocalDateTime/Instant. When a LocalDate/LocalDateTime/Instant
     * is a field of a larger message, the polymorphic marshaller defined in
     * the `avro` or `avrowithschema` object is used.
     */
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
              EncoderUtil.longToByteArray(JavaTimeUtil.localDateTimeToLong(value))
            )

          override def parse(stream: InputStream): LocalDateTime =
            JavaTimeUtil.longToLocalDateTime(
              EncoderUtil.byteArrayToLong(ByteStreams.toByteArray(stream))
            )
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

object avrowithschema {

  implicit val emptyMarshaller: Marshaller[Empty.type] = new Marshaller[Empty.type] {
    override def parse(stream: InputStream): Empty.type = Empty
    override def stream(value: Empty.type)              = new ByteArrayInputStream(Array.empty)
  }

  import com.sksamuel.avro4s._

  /*
   * Marshaller for Avro record types.
   * Warning: it's possible to call this for non-record types (e.g. Int),
   * but it will blow up at runtime.
   */
  implicit def avroWithSchemaMarshallers[A: SchemaFor: Encoder: Decoder]: Marshaller[A] =
    new Marshaller[A] {

      val schema = AvroSchema[A]

      override def parse(stream: InputStream): A = {
        val input: AvroInputStream[A] = AvroInputStream.data[A].from(stream).build
        input.iterator.toList.head
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
        val output: AvroOutputStream[A] = AvroOutputStream.data[A].to(baos).build(schema)
        output.write(value)
        output.close()

        new ByteArrayInputStream(baos.toByteArray)
      }

    }
}
