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

package freestyle.rpc.benchmarks
package shared
package server

import cats.effect.IO
import freestyle.rpc.server.GrpcServer
import org.log4s.Logger

object RPCAvroServer extends AvroImplicits {

  val logger: Logger = org.log4s.getLogger

  def main(args: Array[String]): Unit = {

    logger.info(s"Server is starting ...")

    val avroServer =
      GrpcServer.default[IO](channel.port, grpcConfigsAvro).flatMap(GrpcServer.server[IO])

    avroServer.unsafeRunSync()
  }

}
