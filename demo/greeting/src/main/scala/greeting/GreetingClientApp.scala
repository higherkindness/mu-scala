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

package freestyle.rpc.demo
package greeting

import cats._
import cats.implicits._
import freestyle._
import freestyle.implicits._
import freestyle.rpc.demo.echo_messages._
import runtime.implicits.client._
import greeting.client._
import io.grpc._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

@module
trait ClientAPP {
  val greetingClientM: GreetingClientM
  val echoClientM: EchoClientM
}

object GreetingClientApp {

  val messageRequest = MessageRequest("Freestyle")
  val echoRequest    = EchoRequest("echo...")

  def unaryDemo[F[_]](implicit APP: ClientAPP[F]): FreeS[F, (String, String, String)] = {

    val greetingClientM: GreetingClientM[F] = APP.greetingClientM
    val echoClientM: EchoClientM[F]         = APP.echoClientM

    val defaultOptions = CallOptions.DEFAULT

    for {
      hi   <- greetingClientM.sayHello(messageRequest, defaultOptions)
      bye  <- greetingClientM.sayGoodbye(messageRequest, defaultOptions)
      echo <- echoClientM.echo(echoRequest, defaultOptions)
    } yield {
      println(s"Received -> (${hi.message}, ${bye.message}, ${echo.message})")
      (hi.message, bye.message, echo.message)
    }
  }

  def runProgram[F[_]](implicit M: Monad[F]) = {

    Await.result(unaryDemo[ClientAPP.Op].interpret[Future], Duration.Inf)

    val client = new GreetingClient(host, portNode1)

    // Server streaming RPCs where the client sends a request to the server and
    // gets a stream to read a sequence of messages back. The client reads from
    // the returned stream until there are no more messages.

    client.serverStreamingDemo(messageRequest)

    // Client streaming RPCs where the client writes a sequence of messages and sends them
    // to the server, again using a provided stream. Once the client has finished writing the messages,
    // it waits for the server to read them and return its response.

    client.clientStreamingDemo()

    // Bidirectional streaming RPCs where both sides send a sequence of messages using a read-write stream.
    // The two streams operate independently, so clients and servers can read and write in whatever order
    // they like: for example, the server could wait to receive all the client messages before writing its
    // responses, or it could alternately read a message then write a message, or some other combination
    // of reads and writes. The order of messages in each stream is preserved.

    client.biStreamingDemo()

    (): Unit
  }

  def main(args: Array[String]): Unit = {
    runProgram[Future]
    (): Unit
  }
}
