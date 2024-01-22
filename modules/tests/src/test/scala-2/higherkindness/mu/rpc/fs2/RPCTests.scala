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

package higherkindness.mu.rpc.fs2

import cats.effect.{IO, Resource}
import fs2.Stream
import higherkindness.mu.rpc.common._
import higherkindness.mu.rpc.fs2.Utils.service.ProtoRPCService
import munit.CatsEffectSuite
import org.log4s.{getLogger, Logger}

import java.io.IOException
import scala.concurrent.duration._

class RPCTests extends CatsEffectSuite {

  private val logger: Logger = getLogger

  import higherkindness.mu.rpc.fs2.Utils.database._
  import higherkindness.mu.rpc.fs2.Utils.implicits._

  val retryFiveTimes: retry.RetryPolicy[IO] =
    retry.RetryPolicies.limitRetries[IO](5)

  implicit class IOOps[Result](private val io: IO[Result]) {
    def withRetry: IO[Result] =
      retry
        .retryingOnSomeErrors[Result]
        .apply[IO, Throwable](
          retryFiveTimes,
          {
            case _: IOException => IO.pure(true)
            case _              => IO.pure(false)
          },
          (e, details) =>
            details match {
              case retry.RetryDetails.WillDelayAndRetry(_, retries: Int, _) =>
                IO(logger.info(e)(s"Failed, retried $retries times"))
              case _ => IO.unit
            }
        )(io)
  }

  val behaviourOf: String = "mu-rpc client with fs2.Stream"

  test("mu-rpc server should allow to startup a server and check if it's alive") {
    grpcServer.use(_.isShutdown).withRetry.assertEquals(false)
  }

  test("mu-rpc server should allow to get the port where it's running") {
    grpcServer.use(_.getPort).withRetry.assertEquals(SC.port)
  }

  test(behaviourOf + " be able to run unary services") {
    grpcServer
      .flatMap(_ => muAvroRPCServiceClient)
      .use(_.unary(a1))
      .withRetry
      .assertEquals(c1)
  }

  test(behaviourOf + " be able to run unary services with avro schemas") {
    grpcServer
      .flatMap(_ => muAvroWithSchemaRPCServiceClient)
      .use(_.unaryWithSchema(a1))
      .withRetry
      .assertEquals(c1)
  }

  test(behaviourOf + " be able to run server streaming services") {
    grpcServer
      .flatMap(_ => muProtoRPCServiceClient)
      .use(_.serverStreaming(b1).flatMap(_.compile.toList))
      .withRetry
      .assertEquals(cList)
  }

  test(behaviourOf + " handle errors in server streaming services") {

    def clientProgram(errorCode: String, s: ProtoRPCService[IO]): IO[List[C]] =
      s.serverStreamingWithError(E(a1, errorCode))
        .map(_.handleErrorWith(ex => Stream(C(ex.getMessage, a1))))
        .flatMap(_.compile.toList)

    grpcServer
      .flatMap(_ => muProtoRPCServiceClient)
      .use { s =>
        clientProgram("SE", s)
          .assertEquals(List(C("INVALID_ARGUMENT: SE", a1))) *>
          clientProgram("SRE", s)
            .assertEquals(List(C("INVALID_ARGUMENT: SRE", a1))) *>
          clientProgram("RTE", s)
            .assertEquals(List(C("INTERNAL: RTE", a1))) *>
          clientProgram("Thrown", s)
            .assertEquals(List(C("UNKNOWN: Application error processing RPC", a1)))
      }
      .withRetry
  }

  test(behaviourOf + " be able to run client streaming services") {
    grpcServer
      .flatMap(_ => muProtoRPCServiceClient)
      .use(_.clientStreaming(Stream.fromIterator[IO](aList.iterator, 1)))
      .withRetry
      .assertEquals(dResult33)
  }

  test(behaviourOf + " be able to run client bidirectional streaming services") {
    grpcServer
      .flatMap(_ => muAvroRPCServiceClient)
      .use(
        _.biStreaming(Stream.fromIterator[IO](eList.iterator, 1)).flatMap(_.compile.toList)
      )
      .withRetry
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(
    behaviourOf + " be able to run client bidirectional streaming services with avro schema"
  ) {
    grpcServer
      .flatMap(_ => muAvroWithSchemaRPCServiceClient)
      .use(
        _.biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .withRetry
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(behaviourOf + " be able to run multiple rpc services") {
    val opTimeout: FiniteDuration = 10.seconds
    (for {
      _     <- grpcServer
      avroC <- muAvroRPCServiceClient
      _     <- Resource.eval(avroC.unary(a1).assertEquals(c1))
      _ <- Resource.eval(
        avroC
          .biStreaming(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
          .map(_.distinct)
          .assertEquals(eList)
      )

      avroWSC <- muAvroWithSchemaRPCServiceClient
      _       <- Resource.eval(avroWSC.unaryWithSchema(a1).assertEquals(c1))
      _ <- Resource.eval(
        avroWSC
          .biStreamingWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
          .map(_.distinct)
          .assertEquals(eList)
      )

      protoC <- muProtoRPCServiceClient
      _ <- Resource.eval(
        protoC
          .serverStreaming(b1)
          .flatMap(_.compile.toList)
          .assertEquals(cList)
      )
      _ <- Resource.eval(
        protoC
          .clientStreaming(Stream.fromIterator[IO](aList.iterator, 1))
          .assertEquals(dResult33)
      )
    } yield ()).use_.withRetry.timeoutTo(opTimeout, IO.println("ERROR on multi-op fs2!"))
  }

  val behaviourOfC: String = behaviourOf + " and compression enabled"

  test(behaviourOfC + " be able to run unary services") {
    grpcServer
      .flatMap(_ => muCompressedAvroRPCServiceClient)
      .use(_.unaryCompressed(a1))
      .withRetry
      .assertEquals(c1)
  }

  test(behaviourOfC + " be able to run unary services with avro schema") {
    grpcServer
      .flatMap(_ => muCompressedAvroWithSchemaRPCServiceClient)
      .use(_.unaryCompressedWithSchema(a1))
      .withRetry
      .assertEquals(c1)
  }

  test(behaviourOfC + " be able to run server streaming services") {
    grpcServer
      .flatMap(_ => muCompressedProtoRPCServiceClient)
      .use(_.serverStreamingCompressed(b1).flatMap(_.compile.toList))
      .withRetry
      .assertEquals(cList)
  }

  test(behaviourOfC + " be able to run client streaming services") {
    grpcServer
      .flatMap(_ => muCompressedProtoRPCServiceClient)
      .use(_.clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1)))
      .withRetry
      .assertEquals(dResult33)
  }

  test(behaviourOfC + " be able to run client bidirectional streaming services") {
    grpcServer
      .flatMap(_ => muCompressedAvroRPCServiceClient)
      .use(
        _.biStreamingCompressed(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .withRetry
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(
    behaviourOfC + " be able to run client bidirectional streaming services with avro schema"
  ) {
    grpcServer
      .flatMap(_ => muCompressedAvroWithSchemaRPCServiceClient)
      .use(
        _.biStreamingCompressedWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
      )
      .withRetry
      .map(_.distinct)
      .assertEquals(eList)
  }

  test(behaviourOfC + " be able to run multiple rpc services") {
    val opTimeout: FiniteDuration = 10.seconds
    (for {
      _     <- grpcServer
      avroC <- muCompressedAvroRPCServiceClient
      _     <- Resource.eval(avroC.unaryCompressed(a1).assertEquals(c1))
      _ <- Resource.eval(
        avroC
          .biStreamingCompressed(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
          .map(_.distinct)
          .assertEquals(eList)
      )

      avroWSC <- muCompressedAvroWithSchemaRPCServiceClient
      _ <- Resource.eval(
        avroWSC
          .unaryCompressedWithSchema(a1)
          .assertEquals(c1)
      )
      _ <- Resource.eval(
        avroWSC
          .biStreamingCompressedWithSchema(Stream.fromIterator[IO](eList.iterator, 1))
          .flatMap(_.compile.toList)
          .map(_.distinct)
          .assertEquals(eList)
      )

      protoC <- muCompressedProtoRPCServiceClient
      _ <- Resource.eval(
        protoC
          .serverStreamingCompressed(b1)
          .flatMap(_.compile.toList)
          .assertEquals(cList)
      )
      _ <- Resource.eval(
        protoC
          .clientStreamingCompressed(Stream.fromIterator[IO](aList.iterator, 1))
          .assertEquals(dResult33)
      )
    } yield ()).use_.withRetry.timeout(opTimeout)
  }

}
