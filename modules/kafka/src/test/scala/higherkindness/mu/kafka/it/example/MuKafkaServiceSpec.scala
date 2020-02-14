/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.kafka.it.example

import cats.effect.{ContextShift, IO, Timer}
import fs2.{Pipe, Stream}
import higherkindness.mu.kafka
import higherkindness.mu.kafka.config.KafkaBrokers
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.Success

case class UserAdded(name: String)

class MuKafkaServiceSpec extends AnyFlatSpec with Matchers with Futures with ScalaFutures {
  behavior of "mu Kafka consumer And producer. Kafka is expected to be running." //TODO use embedded kafka for now

  // mu kafka consumer & producer dependencies
  implicit val cs: ContextShift[IO]       = IO.contextShift(global)
  implicit val timer: Timer[IO]           = IO.timer(global)
  implicit val kafkaBrokers: KafkaBrokers = TestConfig.itTestKafkaBrokers
  import higherkindness.mu.format.AvroWithSchema._

  // messages and message processing logic
  val userAddedMessage: UserAdded                           = UserAdded("n")
  val userAddedMessageStream: Stream[IO, Option[UserAdded]] = Stream(Option(userAddedMessage), None)

  // kafka config
  import TestConfig.kafka._

  it should "produce and consume UserAdded" in {
    val consumed: Promise[Unit] = Promise()

    val userAddedMessageProcessor: Pipe[IO, UserAdded, UserAdded] = _.map { userAdded =>
      println(s"Processing $userAdded")
      userAdded shouldBe userAddedMessage //todo deal with the failure case
      consumed.complete(Success(()))
      userAdded
    }

    kafka.consumer(topic, consumerGroup, userAddedMessageProcessor).unsafeRunAsyncAndForget()

    kafka
      .producer(topic, userAddedMessageStream)
      .unsafeRunSync()

    Await.ready(consumed.future, 1 minute)
  }
}
