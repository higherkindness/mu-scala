package higherkindness.mu.rpc.avro

import com.sksamuel.avro4s._
import org.apache.avro._
import org.apache.avro.generic.GenericContainer
import scala.reflect.Typeable

final case class AvroUnion2[T1, T2](value: T1 | T2)
final case class AvroUnion3[T1, T2, T3](value: T1 | T2 | T3)

object Utils {

  def canDecode[T](schema: Schema)(using tgd: TypeGuardedDecoding[T]): Any => Boolean =
    value => tgd.guard(schema).applyOrElse(value, _ => false)

  def decodingException(value: Any, schemas: Schema*): Avro4sDecodingException = {
    val schemaNames = schemas.map(_.getFullName).mkString("[", ", ", "]")
    new Avro4sDecodingException(s"Could not decode union value [$value] (${value.getClass}) using any of these schemas: $schemaNames", value)
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

// TODO a whole load more of these...
