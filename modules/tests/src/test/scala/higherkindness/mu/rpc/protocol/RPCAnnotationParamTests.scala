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

package higherkindness.mu.rpc.protocol

import cats.effect.IO
import cats.syntax.all._
import munit.CatsEffectSuite

class RPCAnnotationParamTests extends CatsEffectSuite {

  case class Request(s: String)
  case class Response(length: Int)

  test("The service annotation should work when only the protocol is specified") {
    @service(Protobuf) trait ServiceDef1[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(Avro) trait ServiceDef2[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(AvroWithSchema) trait ServiceDef3[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }
    implicit val rpcServiceHandler1: ServiceDef1[IO] = new ServiceDef1[IO] {
      override def proto(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler2: ServiceDef2[IO] = new ServiceDef2[IO] {
      override def avro(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler3: ServiceDef3[IO] = new ServiceDef3[IO] {
      override def avroWithSchema(req: Request): IO[Response] = IO.pure(Response(0))
    }
    ServiceDef1
      .bindService[IO]
      .use(_.getServiceDescriptor.getName.pure[IO])
      .assertEquals("ServiceDef1") *>
      ServiceDef2
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("ServiceDef2") *>
      ServiceDef3
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("ServiceDef3")
  }

  test("The service annotation should work when the params are specified in order") {
    @service(Protobuf, Identity, Some("my.package"), Unchanged) trait ServiceDef1[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(Avro, Gzip, Some("my.package"), Unchanged) trait ServiceDef2[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(AvroWithSchema, Identity, Some("my.package"), Capitalize) trait ServiceDef3[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }
    implicit val rpcServiceHandler1: ServiceDef1[IO] = new ServiceDef1[IO] {
      override def proto(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler2: ServiceDef2[IO] = new ServiceDef2[IO] {
      override def avro(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler3: ServiceDef3[IO] = new ServiceDef3[IO] {
      override def avroWithSchema(req: Request): IO[Response] = IO.pure(Response(0))
    }
    ServiceDef1
      .bindService[IO]
      .use(_.getServiceDescriptor.getName.pure[IO])
      .assertEquals("my.package.ServiceDef1") *>
      ServiceDef2
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef2") *>
      ServiceDef3
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef3")
  }

  test("The service annotation should work when the params are specified with name") {
    @service(
      serializationType = Protobuf,
      compressionType = Identity,
      namespace = Some("my.package"),
      methodNameStyle = Unchanged
    ) trait ServiceDef1[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(
      serializationType = Avro,
      compressionType = Gzip,
      namespace = Some("my.package"),
      methodNameStyle = Unchanged
    ) trait ServiceDef2[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(
      serializationType = AvroWithSchema,
      compressionType = Identity,
      namespace = Some("my.package"),
      methodNameStyle = Capitalize
    ) trait ServiceDef3[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }
    implicit val rpcServiceHandler1: ServiceDef1[IO] = new ServiceDef1[IO] {
      override def proto(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler2: ServiceDef2[IO] = new ServiceDef2[IO] {
      override def avro(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler3: ServiceDef3[IO] = new ServiceDef3[IO] {
      override def avroWithSchema(req: Request): IO[Response] = IO.pure(Response(0))
    }
    ServiceDef1
      .bindService[IO]
      .use(_.getServiceDescriptor.getName.pure[IO])
      .assertEquals("my.package.ServiceDef1") *>
      ServiceDef2
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef2") *>
      ServiceDef3
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef3")
  }

  test(
    "The service annotation should work when the params are specified with name and in different order"
  ) {
    @service(
      compressionType = Identity,
      namespace = Some("my.package"),
      serializationType = Protobuf,
      methodNameStyle = Unchanged
    ) trait ServiceDef1[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(
      compressionType = Gzip,
      namespace = Some("my.package"),
      methodNameStyle = Unchanged,
      serializationType = Avro
    ) trait ServiceDef2[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(
      namespace = Some("my.package"),
      methodNameStyle = Capitalize,
      serializationType = AvroWithSchema,
      compressionType = Identity
    ) trait ServiceDef3[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }
    implicit val rpcServiceHandler1: ServiceDef1[IO] = new ServiceDef1[IO] {
      override def proto(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler2: ServiceDef2[IO] = new ServiceDef2[IO] {
      override def avro(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler3: ServiceDef3[IO] = new ServiceDef3[IO] {
      override def avroWithSchema(req: Request): IO[Response] = IO.pure(Response(0))
    }
    ServiceDef1
      .bindService[IO]
      .use(_.getServiceDescriptor.getName.pure[IO])
      .assertEquals("my.package.ServiceDef1") *>
      ServiceDef2
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef2") *>
      ServiceDef3
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef3")
  }

  test(
    "The service annotation should work when some params are specified in order and others with name"
  ) {
    @service(
      Protobuf,
      Identity,
      namespace = Some("my.package"),
      methodNameStyle = Unchanged
    ) trait ServiceDef1[F[_]] {
      def proto(req: Request): F[Response]
    }
    @service(Avro, Gzip, Some("my.package"), methodNameStyle = Unchanged) trait ServiceDef2[F[_]] {
      def avro(req: Request): F[Response]
    }
    @service(
      serializationType = AvroWithSchema,
      Identity,
      Some("my.package"),
      methodNameStyle = Capitalize
    ) trait ServiceDef3[F[_]] {
      def avroWithSchema(req: Request): F[Response]
    }
    implicit val rpcServiceHandler1: ServiceDef1[IO] = new ServiceDef1[IO] {
      override def proto(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler2: ServiceDef2[IO] = new ServiceDef2[IO] {
      override def avro(req: Request): IO[Response] = IO.pure(Response(0))
    }
    implicit val rpcServiceHandler3: ServiceDef3[IO] = new ServiceDef3[IO] {
      override def avroWithSchema(req: Request): IO[Response] = IO.pure(Response(0))
    }
    ServiceDef1
      .bindService[IO]
      .use(_.getServiceDescriptor.getName.pure[IO])
      .assertEquals("my.package.ServiceDef1") *>
      ServiceDef2
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef2") *>
      ServiceDef3
        .bindService[IO]
        .use(_.getServiceDescriptor.getName.pure[IO])
        .assertEquals("my.package.ServiceDef3")
  }
}
