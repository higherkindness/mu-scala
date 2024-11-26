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

import com.sksamuel.avro4s._
import org.apache.avro._
import scala.reflect.Typeable

final case class AvroUnion2[T1, T2](value: T1 | T2)
final case class AvroUnion3[T1, T2, T3](value: T1 | T2 | T3)
final case class AvroUnion4[T1, T2, T3, T4](value: T1 | T2 | T3 | T4)
final case class AvroUnion5[T1, T2, T3, T4, T5](value: T1 | T2 | T3 | T4 | T5)
final case class AvroUnion6[T1, T2, T3, T4, T5, T6](value: T1 | T2 | T3 | T4 | T5 | T6)
final case class AvroUnion7[T1, T2, T3, T4, T5, T6, T7](value: T1 | T2 | T3 | T4 | T5 | T6 | T7)
final case class AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](
    value: T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8
)

object Utils {

  def canDecode[T](schema: Schema)(using tgd: TypeGuardedDecoding[T]): Any => Boolean =
    value => tgd.guard(schema).applyOrElse(value, _ => false)

  def decodingException(value: Any, schemas: Schema*): Avro4sDecodingException = {
    val schemaNames = schemas.map(_.getFullName).mkString("[", ", ", "]")
    new Avro4sDecodingException(
      s"Could not decode union value [$value] (${value.getClass}) using any of these schemas: $schemaNames",
      value
    )
  }

}

import Utils._

object AvroUnion2 {

  given [
      T1: SchemaFor,
      T2: SchemaFor
  ]: SchemaFor[AvroUnion2[T1, T2]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion2[T1, T2]] with
    def encode(schema: Schema): AvroUnion2[T1, T2] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion2[T1, T2]] with
    def decode(schema: Schema): Any => AvroUnion2[T1, T2] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion2[T1, T2](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion2[T1, T2](decoded)
        } else {
          throw decodingException(value, schema1, schema2)
        }
      }
    }

}

object AvroUnion3 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor
  ]: SchemaFor[AvroUnion3[T1, T2, T3]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion3[T1, T2, T3]] with
    def encode(schema: Schema): AvroUnion3[T1, T2, T3] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion3[T1, T2, T3]] with
    def decode(schema: Schema): Any => AvroUnion3[T1, T2, T3] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion3[T1, T2, T3](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion3[T1, T2, T3](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion3[T1, T2, T3](decoded)
        } else {
          throw decodingException(value, schema1, schema2, schema3)
        }
      }
    }

}

object AvroUnion4 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor,
      T4: SchemaFor
  ]: SchemaFor[AvroUnion4[T1, T2, T3, T4]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .and
        .`type`(AvroSchema[T4])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder,
      T4: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion4[T1, T2, T3, T4]] with
    def encode(schema: Schema): AvroUnion4[T1, T2, T3, T4] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
        case v: T4 => Encoder[T4].encode(AvroSchema[T4])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding,
      T4: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion4[T1, T2, T3, T4]] with
    def decode(schema: Schema): Any => AvroUnion4[T1, T2, T3, T4] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]
      val schema4 = AvroSchema[T4]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion4[T1, T2, T3, T4](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion4[T1, T2, T3, T4](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion4[T1, T2, T3, T4](decoded)
        } else if (canDecode[T4](schema4)(value)) {
          val decoded: T4 = Decoder[T4].decode(schema4).apply(value)
          AvroUnion4[T1, T2, T3, T4](decoded)
        } else {
          throw decodingException(value, schema1, schema2, schema3, schema4)
        }
      }
    }

}

object AvroUnion5 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor,
      T4: SchemaFor,
      T5: SchemaFor
  ]: SchemaFor[AvroUnion5[T1, T2, T3, T4, T5]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .and
        .`type`(AvroSchema[T4])
        .and
        .`type`(AvroSchema[T5])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder,
      T4: Typeable: SchemaFor: Encoder,
      T5: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion5[T1, T2, T3, T4, T5]] with
    def encode(schema: Schema): AvroUnion5[T1, T2, T3, T4, T5] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
        case v: T4 => Encoder[T4].encode(AvroSchema[T4])(v)
        case v: T5 => Encoder[T5].encode(AvroSchema[T5])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding,
      T4: SchemaFor: Decoder: TypeGuardedDecoding,
      T5: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion5[T1, T2, T3, T4, T5]] with
    def decode(schema: Schema): Any => AvroUnion5[T1, T2, T3, T4, T5] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]
      val schema4 = AvroSchema[T4]
      val schema5 = AvroSchema[T5]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion5[T1, T2, T3, T4, T5](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion5[T1, T2, T3, T4, T5](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion5[T1, T2, T3, T4, T5](decoded)
        } else if (canDecode[T4](schema4)(value)) {
          val decoded: T4 = Decoder[T4].decode(schema4).apply(value)
          AvroUnion5[T1, T2, T3, T4, T5](decoded)
        } else if (canDecode[T5](schema5)(value)) {
          val decoded: T5 = Decoder[T5].decode(schema5).apply(value)
          AvroUnion5[T1, T2, T3, T4, T5](decoded)
        } else {
          throw decodingException(value, schema1, schema2, schema3, schema4, schema5)
        }
      }
    }

}

object AvroUnion6 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor,
      T4: SchemaFor,
      T5: SchemaFor,
      T6: SchemaFor
  ]: SchemaFor[AvroUnion6[T1, T2, T3, T4, T5, T6]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .and
        .`type`(AvroSchema[T4])
        .and
        .`type`(AvroSchema[T5])
        .and
        .`type`(AvroSchema[T6])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder,
      T4: Typeable: SchemaFor: Encoder,
      T5: Typeable: SchemaFor: Encoder,
      T6: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion6[T1, T2, T3, T4, T5, T6]] with
    def encode(schema: Schema): AvroUnion6[T1, T2, T3, T4, T5, T6] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
        case v: T4 => Encoder[T4].encode(AvroSchema[T4])(v)
        case v: T5 => Encoder[T5].encode(AvroSchema[T5])(v)
        case v: T6 => Encoder[T6].encode(AvroSchema[T6])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding,
      T4: SchemaFor: Decoder: TypeGuardedDecoding,
      T5: SchemaFor: Decoder: TypeGuardedDecoding,
      T6: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion6[T1, T2, T3, T4, T5, T6]] with
    def decode(schema: Schema): Any => AvroUnion6[T1, T2, T3, T4, T5, T6] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]
      val schema4 = AvroSchema[T4]
      val schema5 = AvroSchema[T5]
      val schema6 = AvroSchema[T6]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else if (canDecode[T4](schema4)(value)) {
          val decoded: T4 = Decoder[T4].decode(schema4).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else if (canDecode[T5](schema5)(value)) {
          val decoded: T5 = Decoder[T5].decode(schema5).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else if (canDecode[T6](schema6)(value)) {
          val decoded: T6 = Decoder[T6].decode(schema6).apply(value)
          AvroUnion6[T1, T2, T3, T4, T5, T6](decoded)
        } else {
          throw decodingException(value, schema1, schema2, schema3, schema4, schema5, schema6)
        }
      }
    }

}

object AvroUnion7 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor,
      T4: SchemaFor,
      T5: SchemaFor,
      T6: SchemaFor,
      T7: SchemaFor
  ]: SchemaFor[AvroUnion7[T1, T2, T3, T4, T5, T6, T7]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .and
        .`type`(AvroSchema[T4])
        .and
        .`type`(AvroSchema[T5])
        .and
        .`type`(AvroSchema[T6])
        .and
        .`type`(AvroSchema[T7])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder,
      T4: Typeable: SchemaFor: Encoder,
      T5: Typeable: SchemaFor: Encoder,
      T6: Typeable: SchemaFor: Encoder,
      T7: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion7[T1, T2, T3, T4, T5, T6, T7]] with
    def encode(schema: Schema): AvroUnion7[T1, T2, T3, T4, T5, T6, T7] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
        case v: T4 => Encoder[T4].encode(AvroSchema[T4])(v)
        case v: T5 => Encoder[T5].encode(AvroSchema[T5])(v)
        case v: T6 => Encoder[T6].encode(AvroSchema[T6])(v)
        case v: T7 => Encoder[T7].encode(AvroSchema[T7])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding,
      T4: SchemaFor: Decoder: TypeGuardedDecoding,
      T5: SchemaFor: Decoder: TypeGuardedDecoding,
      T6: SchemaFor: Decoder: TypeGuardedDecoding,
      T7: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion7[T1, T2, T3, T4, T5, T6, T7]] with
    def decode(schema: Schema): Any => AvroUnion7[T1, T2, T3, T4, T5, T6, T7] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]
      val schema4 = AvroSchema[T4]
      val schema5 = AvroSchema[T5]
      val schema6 = AvroSchema[T6]
      val schema7 = AvroSchema[T7]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T4](schema4)(value)) {
          val decoded: T4 = Decoder[T4].decode(schema4).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T5](schema5)(value)) {
          val decoded: T5 = Decoder[T5].decode(schema5).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T6](schema6)(value)) {
          val decoded: T6 = Decoder[T6].decode(schema6).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else if (canDecode[T7](schema7)(value)) {
          val decoded: T7 = Decoder[T7].decode(schema7).apply(value)
          AvroUnion7[T1, T2, T3, T4, T5, T6, T7](decoded)
        } else {
          throw decodingException(
            value,
            schema1,
            schema2,
            schema3,
            schema4,
            schema5,
            schema6,
            schema7
          )
        }
      }
    }

}

object AvroUnion8 {

  given [
      T1: SchemaFor,
      T2: SchemaFor,
      T3: SchemaFor,
      T4: SchemaFor,
      T5: SchemaFor,
      T6: SchemaFor,
      T7: SchemaFor,
      T8: SchemaFor
  ]: SchemaFor[AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8]] with
    def schema: Schema =
      SchemaBuilder.unionOf
        .`type`(AvroSchema[T1])
        .and
        .`type`(AvroSchema[T2])
        .and
        .`type`(AvroSchema[T3])
        .and
        .`type`(AvroSchema[T4])
        .and
        .`type`(AvroSchema[T5])
        .and
        .`type`(AvroSchema[T6])
        .and
        .`type`(AvroSchema[T7])
        .and
        .`type`(AvroSchema[T8])
        .endUnion

  given [
      T1: Typeable: SchemaFor: Encoder,
      T2: Typeable: SchemaFor: Encoder,
      T3: Typeable: SchemaFor: Encoder,
      T4: Typeable: SchemaFor: Encoder,
      T5: Typeable: SchemaFor: Encoder,
      T6: Typeable: SchemaFor: Encoder,
      T7: Typeable: SchemaFor: Encoder,
      T8: Typeable: SchemaFor: Encoder
  ]: Encoder[AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8]] with
    def encode(schema: Schema): AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8] => Any = { union =>
      union.value match {
        case v: T1 => Encoder[T1].encode(AvroSchema[T1])(v)
        case v: T2 => Encoder[T2].encode(AvroSchema[T2])(v)
        case v: T3 => Encoder[T3].encode(AvroSchema[T3])(v)
        case v: T4 => Encoder[T4].encode(AvroSchema[T4])(v)
        case v: T5 => Encoder[T5].encode(AvroSchema[T5])(v)
        case v: T6 => Encoder[T6].encode(AvroSchema[T6])(v)
        case v: T7 => Encoder[T7].encode(AvroSchema[T7])(v)
        case v: T8 => Encoder[T8].encode(AvroSchema[T8])(v)
      }
    }

  given [
      T1: SchemaFor: Decoder: TypeGuardedDecoding,
      T2: SchemaFor: Decoder: TypeGuardedDecoding,
      T3: SchemaFor: Decoder: TypeGuardedDecoding,
      T4: SchemaFor: Decoder: TypeGuardedDecoding,
      T5: SchemaFor: Decoder: TypeGuardedDecoding,
      T6: SchemaFor: Decoder: TypeGuardedDecoding,
      T7: SchemaFor: Decoder: TypeGuardedDecoding,
      T8: SchemaFor: Decoder: TypeGuardedDecoding
  ]: Decoder[AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8]] with
    def decode(schema: Schema): Any => AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8] = {
      val schema1 = AvroSchema[T1]
      val schema2 = AvroSchema[T2]
      val schema3 = AvroSchema[T3]
      val schema4 = AvroSchema[T4]
      val schema5 = AvroSchema[T5]
      val schema6 = AvroSchema[T6]
      val schema7 = AvroSchema[T7]
      val schema8 = AvroSchema[T8]

      { value =>
        if (canDecode[T1](schema1)(value)) {
          val decoded: T1 = Decoder[T1].decode(schema1).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T2](schema2)(value)) {
          val decoded: T2 = Decoder[T2].decode(schema2).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T3](schema3)(value)) {
          val decoded: T3 = Decoder[T3].decode(schema3).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T4](schema4)(value)) {
          val decoded: T4 = Decoder[T4].decode(schema4).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T5](schema5)(value)) {
          val decoded: T5 = Decoder[T5].decode(schema5).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T6](schema6)(value)) {
          val decoded: T6 = Decoder[T6].decode(schema6).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T7](schema7)(value)) {
          val decoded: T7 = Decoder[T7].decode(schema7).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else if (canDecode[T8](schema8)(value)) {
          val decoded: T8 = Decoder[T8].decode(schema8).apply(value)
          AvroUnion8[T1, T2, T3, T4, T5, T6, T7, T8](decoded)
        } else {
          throw decodingException(
            value,
            schema1,
            schema2,
            schema3,
            schema4,
            schema5,
            schema6,
            schema7,
            schema8
          )
        }
      }
    }

}
