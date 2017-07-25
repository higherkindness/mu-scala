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
package demo
package protocolgen
package runtime

import cats.{~>, Comonad}
import freestyle.rpc.demo.protocolgen.protocols.{GreetingService, MessageReply}
import io.grpc.stub.StreamObserver
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait CommonImplicits {

  implicit val ec: ExecutionContext         = ExecutionContext.Implicits.global
  implicit val S: monix.execution.Scheduler = monix.execution.Scheduler.Implicits.global

}

object server {

  trait Implicits extends CommonImplicits {

    import cats.implicits._
    import freestyle.rpc.server._
    import freestyle.rpc.server.handlers._
    import freestyle.rpc.server.implicits._
    import freestyle.rpc.client.implicits._

    implicit val finiteDuration: FiniteDuration = 5.seconds

    implicit def futureComonad(
        implicit ec: ExecutionContext,
        atMost: FiniteDuration): Comonad[Future] =
      new Comonad[Future] {
        def extract[A](x: Future[A]): A =
          Await.result(x, atMost)

        override def coflatMap[A, B](fa: Future[A])(f: (Future[A]) => B): Future[B] = Future(f(fa))

        override def map[A, B](fa: Future[A])(f: (A) => B): Future[B] = fa.map(f)
      }

    implicit val greetingServiceHandler: GreetingService.Handler[Future] =
      new GreetingService.Handler[Future] {
        override protected[this] def sayHello(
            msg: protocols.MessageRequest): Future[protocols.MessageReply] =
          Future.successful(MessageReply("hello", List(1, 2, 3, 4, 5)))

        override protected[this] def lotsOfReplies(
            msg: protocols.MessageRequest): Future[Observable[protocols.MessageReply]] = ???

        override protected[this] def lotsOfGreetings(
            msg: Observable[protocols.MessageRequest]): Future[MessageReply] = ???

        override protected[this] def bidiHello(msg: Observable[protocols.MessageRequest]): Future[
          Observable[protocols.MessageReply]] = ???
      }

    val grpcConfigs: List[GrpcConfig] = List(
      AddService(GreetingService.bindService[GreetingService.Op, Future])
    )

    val conf: ServerW = ServerW(50051, grpcConfigs)

    implicit val grpcServerHandler: GrpcServer.Op ~> Future =
      new GrpcServerHandler[Future] andThen
        new GrpcKInterpreter[Future](conf.server)

  }

  object implicits extends Implicits

}
