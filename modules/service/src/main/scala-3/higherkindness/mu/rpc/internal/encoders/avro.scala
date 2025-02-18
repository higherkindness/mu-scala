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

package higherkindness.mu.rpc
package internal.encoders

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import higherkindness.mu.rpc.protocol.Empty
import io.grpc.MethodDescriptor.Marshaller

object avro {

  given Marshaller[Empty.type] with
    override def parse(stream: InputStream): Empty.type = Empty
    override def stream(value: Empty.type)              = new ByteArrayInputStream(Array.empty)

  import com.sksamuel.avro4s._

  /*
   * Marshaller for Avro record types.
   * Warning: it's possible to call this for non-record types (e.g. Int),
   * but it will blow up at runtime.
   */
  given [A: SchemaFor: Encoder: Decoder]: Marshaller[A] with

    override def parse(stream: InputStream): A = {
      val input: AvroInputStream[A] = AvroInputStream.binary[A].from(stream).build(AvroSchema[A])
      input.iterator.toList.head
    }

    override def stream(value: A): InputStream = {
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val output: AvroOutputStream[A] = AvroOutputStream.binary[A].to(baos).build()
      output.write(value)
      output.close()

      new ByteArrayInputStream(baos.toByteArray)
    }

}

object avrowithschema {

  implicit val emptyMarshaller: Marshaller[Empty.type] = new Marshaller[Empty.type] {
    override def parse(stream: InputStream): Empty.type = Empty
    override def stream(value: Empty.type)              = new ByteArrayInputStream(Array.empty)
  }

  import com.sksamuel.avro4s._
  import org.apache.avro.file.DataFileStream
  import org.apache.avro.generic.{GenericDatumReader, GenericRecord}

  /*
   * Marshaller for Avro record types.
   * Warning: it's possible to call this for non-record types (e.g. Int),
   * but it will blow up at runtime.
   */
  implicit def avroWithSchemaMarshallers[A: SchemaFor: Encoder: Decoder]: Marshaller[A] =
    new Marshaller[A] {

      val schema = AvroSchema[A]

      override def parse(stream: InputStream): A = {
        val dfs = new DataFileStream(stream, new GenericDatumReader[GenericRecord](schema))
        FromRecord[A](schema).from(dfs.next())
      }

      override def stream(value: A): InputStream = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
        val output: AvroOutputStream[A] = AvroOutputStream.data[A].to(baos).build()
        output.write(value)
        output.close()

        new ByteArrayInputStream(baos.toByteArray)
      }

    }
}
