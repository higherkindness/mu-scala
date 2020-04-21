/*
 * Copyright 2017-2020 47 Degrees <http://47deg.com>
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

package higherkindness.mu.rpc.protocol.legacy

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.ByteBuffer

import com.google.common.io.ByteStreams
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import higherkindness.mu.rpc.internal.util.BigDecimalUtil
import io.grpc.MethodDescriptor.Marshaller
import org.apache.avro.Schema

object avro {

  import AvroDecimalCompatUtils._

  implicit object bigDecimalSchemaFor extends SchemaFor[AvroDecimalCompat] {
    def schema(fm: FieldMapper): Schema = Schema.create(Schema.Type.BYTES)
  }

  implicit object bigDecimalDecoder extends Decoder[AvroDecimalCompat] {
    def decode(value: Any, schema: Schema, fm: FieldMapper): AvroDecimalCompat =
      AvroDecimalCompat(BigDecimalUtil.byteToBigDecimal(value.asInstanceOf[ByteBuffer].array()))
  }

  implicit object bigDecimalEncoder extends Encoder[AvroDecimalCompat] {
    def encode(value: AvroDecimalCompat, schema: Schema, fm: FieldMapper): ByteBuffer =
      ByteBuffer.wrap(BigDecimalUtil.bigDecimalToByte(value.toBigDecimal))
  }

  implicit val bigDecimalMarshaller: Marshaller[AvroDecimalCompat] =
    new Marshaller[AvroDecimalCompat] {
      override def stream(value: AvroDecimalCompat): InputStream =
        new ByteArrayInputStream(BigDecimalUtil.bigDecimalToByte(value.toBigDecimal))

      override def parse(stream: InputStream): AvroDecimalCompat =
        AvroDecimalCompat(BigDecimalUtil.byteToBigDecimal(ByteStreams.toByteArray(stream)))
    }

}
