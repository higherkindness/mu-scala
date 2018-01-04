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

import cats.{~>, Monad}
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.logging._
import freestyle.free.loggingJVM.implicits._
import freestyle.rpc.server.handlers.GrpcServerHandler

@module
trait GrpcServerApp {
  val server: GrpcServer
  val log: LoggingM
}

trait ServerImplicits {

  implicit def grpcServerHandler[M[_]: Capture](implicit SW: ServerW): GrpcServer.Op ~> M =
    new GrpcServerHandler[M] andThen new GrpcKInterpreter[M](SW.server)

}

trait Syntax {

  implicit class serverOps(server: FreeS[GrpcServerApp.Op, Unit]) {

    def bootstrapM[M[_]: Monad](implicit handler: GrpcServer.Op ~> M): M[Unit] =
      server.interpret[M]

  }
}

trait Helpers {

  def server[M[_]](implicit APP: GrpcServerApp[M]): FreeS[M, Unit] = {
    val server = APP.server
    val log    = APP.log
    for {
      _    <- server.start()
      port <- server.getPort
      _    <- log.info(s"Server started, listening on $port")
      _    <- server.awaitTermination()
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
    with loggingJVM.Implicits
