/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.{~>, Monad}
import freestyle._
import freestyle.implicits._

import scala.concurrent.Future

trait Syntax {

  implicit def serverOps(server: FreeS[GrpcServer.Op, Unit]): ServerOps = new ServerOps(server)

  final class ServerOps(server: FreeS[GrpcServer.Op, Unit]) {

    def bootstrapM[M[_]: Monad](implicit handler: GrpcServer.Op ~> M): M[Unit] =
      server.interpret[M]

    def bootstrapFuture(
        implicit MF: Monad[Future],
        handler: GrpcServer.Op ~> Future): Future[Unit] =
      server.interpret[Future]

  }
}

object implicits extends CaptureInstances with Syntax {

  def server[M[_]](implicit app: GrpcServer[M]): FreeS[M, Unit] = {
    for {
      _ <- app.start()
      _ <- app.awaitTermination()
    } yield ()
  }
}
