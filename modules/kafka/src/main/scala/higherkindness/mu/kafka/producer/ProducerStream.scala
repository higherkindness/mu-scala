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

package higherkindness.mu.kafka.producer

import cats.effect._
import fs2._
import fs2.concurrent.Queue
import fs2.kafka.{produce, _}
import higherkindness.mu.format.Encoder
import higherkindness.mu.kafka.{ByteArrayProducerResult, PublishToKafka}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.language.higherKinds

object ProducerStream {

//  def apply[F[_], A](
//      broker: String,
//      topic: String,
//      messageQueue: Stream[F, Queue[F, Option[A]]]
//  )(
//      implicit contextShift: ContextShift[F],
//      concurrentEffect: ConcurrentEffect[F],
//      timer: Timer[F],
//      encoder: Encoder[A]
//  ): F[List[ByteArrayProducerResult]] =
//    (for {
//      queue                        <- messageQueue
//      producerStream               <- streamWithLogger(broker, topic, queue)
//    } yield producerStream).compile.toList

  def withLogger[F[_], A](
      broker: String,
      topic: String,
      queue: Queue[F, Option[A]]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      encoder: Encoder[A]
  ): Stream[F, Queue[F, Option[A]]] =
    for {
      implicit0(logger: Logger[F]) <- Stream.eval(Slf4jLogger.create[F])
      _                            <- stream(broker, topic, queue)
    } yield queue

  def pipe[F[_], A](broker: String, topic: String)(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      sync: Sync[F],
      encoder: Encoder[A]
  ): Stream[F, Option[A]] => Stream[F, ByteArrayProducerResult] =
    as =>
      for {
        implicit0(logger: Logger[F]) <- Stream.eval(Slf4jLogger.create[F])
        result <- as
          .flatMap(a => Stream.eval(Logger[F].info(s"Dequeued $a")).map(_ => a))
          .unNoneTerminate // idiomatic way to terminate a fs2 stream
          .evalMap(a =>
            concurrentEffect.delay(
              ProducerRecords.one(ProducerRecord(topic, "dummy-key", encoder.encode(a)))
            )
          )
          .covary[F]
          .through(produce(higherkindness.mu.kafka.producer.Settings(broker)))
          .flatMap(result =>
            Stream
              .eval(
                Logger[F].info(
                  result.records.head
                    .fold("Error: ProducerResult contained empty records.")(a => s"Published $a")
                )
              )
              .flatMap(_ => Stream.eval(sync.delay(result)))
          )
      } yield result

  def stream[F[_]: Logger, A](
      broker: String,
      topic: String,
      queue: Queue[F, Option[A]]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      sync: Sync[F],
      encoder: Encoder[A]
  ): Stream[F, ByteArrayProducerResult] =
    stream(produce(higherkindness.mu.kafka.producer.Settings(broker)))(topic, queue)

  def stream[F[_]: Logger, A](
      publishToKafka: PublishToKafka[F]
  )(topic: String, queue: Queue[F, Option[A]])(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      sync: Sync[F],
      encoder: Encoder[A]
  ): Stream[F, ByteArrayProducerResult] =
    queue.dequeue
      .flatMap(a => Stream.eval(Logger[F].info(s"Dequeued $a")).map(_ => a))
      .unNoneTerminate // idiomatic way to terminate a fs2 stream
      .evalMap(a =>
        concurrentEffect.delay(
          ProducerRecords.one(ProducerRecord(topic, "dummy-key", encoder.encode(a)))
        )
      )
      .covary[F]
      .through(publishToKafka)
      .flatMap(result =>
        Stream
          .eval(
            Logger[F].info(
              result.records.head
                .fold("Error: ProducerResult contained empty records.")(a => s"Published $a")
            )
          )
          .flatMap(_ => Stream.eval(sync.delay(result)))
      )
}
