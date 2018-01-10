/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.rpc
package server

import cats.effect.Sync
import cats.{~>, Monad}
import freestyle.free._
import freestyle.rpc.async.RPCAsyncImplicits
import freestyle.rpc.server.handlers.GrpcServerHandler

trait ServerImplicits {

  implicit def grpcServerHandler[M[_]: Capture](implicit SW: ServerW): GrpcServer.Op ~> M =
    new GrpcServerHandler[M] andThen new GrpcKInterpreter[M](SW.server)

}

trait Syntax {

  implicit class serverOps(server: FreeS[GrpcServer.Op, Unit]) {

    def bootstrapM[M[_]: Monad](implicit handler: GrpcServer.Op ~> M): M[Unit] =
      server.interpret[M]

  }
}

trait IOCapture {

  implicit def syncCapture[F[_]](implicit F: Sync[F]): Capture[F] =
    new Capture[F] { def capture[A](a: => A): F[A] = F.delay(a) }
}

trait Helpers {

  def server[M[_]](implicit S: GrpcServer[M]): FreeS[M, Unit] = {
    for {
      _ <- S.start()
      _ <- S.getPort
      _ <- S.awaitTermination()
    } yield ()
  }

}

object implicits
    extends CaptureInstances
    with IOCapture
    with RPCAsyncImplicits
    with Syntax
    with Helpers
    with ServerImplicits
    with Interpreters
    with FreeSInstances
