package higherkindness.mu.tests.rpc

import cats.effect._
import io.grpc.Status

class ServiceImpl extends AvroRPCService[IO] {

  def hello(req: Request): IO[Response] =
    if (req == TestData.request) {
      IO.pure(Response(req.a, req.d))
    } else {
      IO.raiseError(
        Status.INTERNAL.withDescription("Request did not match what I expected").asException
      )
    }

}
