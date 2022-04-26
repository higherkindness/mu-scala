package higherkindness.mu.tests.rpc.ssl

import cats.effect._
import io.grpc.Status

class ServiceImpl extends SSLService[IO] {

  def hello(req: Request): IO[Response] =
    IO.pure(Response(req.a))

}
