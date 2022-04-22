package higherkindness.mu.rpc.avro

import munit.FunSuite
import com.sksamuel.avro4s._
import org.apache.avro._
import scala.jdk.CollectionConverters._
import java.io._

case class MyRecord(a: Int, b: Boolean)

class AvroUnionsSpec extends FunSuite {

  type U3 = AvroUnion3[String, Int, MyRecord]

  test("AvroUnion3 schema") {
    val schema = AvroSchema[U3]
    assertEquals(schema.isUnion, true)

    val types = schema.getTypes.asScala
    assertEquals(types(0).getFullName, "string")
    assertEquals(types(1).getFullName, "int")
    assertEquals(types(2).getFullName, "higherkindness.mu.rpc.avro.MyRecord")
  }

  test("AvroUnion3 binary round-trip (string)") {
    val union = AvroUnion3[String, Int, MyRecord]("hello")

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[U3](AvroSchema[U3], Encoder[U3])
      .to(baos)
      .build()
    out.write(union)
    out.close()

    val bytes         = baos.toByteArray.toList
    val expectedBytes = List(0, 10, 104, 101, 108, 108, 111).map(_.toByte)
    assertEquals(bytes, expectedBytes)

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[U3]
      .from(bais)
      .build(AvroSchema[U3])
    val roundTrippedUnion = in.iterator.next()
    in.close()

    assertEquals(roundTrippedUnion, union)
  }

  test("AvroUnion3 binary round-trip (int)") {
    val union = AvroUnion3[String, Int, MyRecord](42)

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[U3](AvroSchema[U3], Encoder[U3])
      .to(baos)
      .build()
    out.write(union)
    out.close()

    val bytes         = baos.toByteArray.toList
    val expectedBytes = List(2, 84).map(_.toByte)
    assertEquals(bytes, expectedBytes)

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[U3]
      .from(bais)
      .build(AvroSchema[U3])
    val roundTrippedUnion = in.iterator.next()
    in.close()

    assertEquals(roundTrippedUnion, union)
  }

  test("AvroUnion3 binary round-trip (record)") {
    val union = AvroUnion3[String, Int, MyRecord](MyRecord(1, true))

    val baos = new ByteArrayOutputStream()
    val out = AvroOutputStream
      .binary[U3](AvroSchema[U3], Encoder[U3])
      .to(baos)
      .build()
    out.write(union)
    out.close()

    val bytes         = baos.toByteArray.toList
    val expectedBytes = List(4, 2, 1).map(_.toByte)
    assertEquals(bytes, expectedBytes)

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = AvroInputStream
      .binary[U3]
      .from(bais)
      .build(AvroSchema[U3])
    val roundTrippedUnion = in.iterator.next()
    in.close()

    assertEquals(roundTrippedUnion, union)
  }

}
