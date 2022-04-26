package higherkindness.mu.tests.rpc

import cats.effect._
import _root_.fs2._
import higherkindness.mu.tests.models._
import _root_.io.grpc.Status

class ServiceImpl extends ProtoRPCService[IO] {

  def unary(req: A): IO[C] =
    IO.pure(C("hello", Some(req)))

  def clientStreaming(req: Stream[IO, A]): IO[D] =
    req.compile.count.map(x => D(x.toInt))

  def serverStreaming(req: B): IO[Stream[IO, C]] =
    IO.pure(Stream(C("first", req.a1), C("second", req.a2)))

  def serverStreamingWithError(req: E): IO[Stream[IO, C]] = {
    val stream: Stream[IO, C] = req.foo match {
      case "SE" =>
        Stream.raiseError[IO](Status.INVALID_ARGUMENT.withDescription(req.foo).asException)
      case "SRE" =>
        Stream.raiseError[IO](Status.INVALID_ARGUMENT.withDescription(req.foo).asRuntimeException)
      case "RTE" =>
        Stream.raiseError[IO](new IllegalArgumentException(req.foo))
      case _ =>
        sys.error(req.foo)
    }
    IO.pure(stream)
  }

  def bidiStreaming(req: Stream[IO, E]): IO[Stream[IO, B]] =
    IO.pure(req.map(e => B(e.a, e.a)))

}
