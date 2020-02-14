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

package higherkindness.mu

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.{Pipe, Stream}
import fs2.kafka.{ProducerRecords, ProducerResult}
import higherkindness.mu.format.{Decoder, Encoder}
import higherkindness.mu.kafka.config.KafkaBrokers

package object kafka {
  type ByteArrayProducerResult  = ProducerResult[String, Array[Byte], Unit]
  type ByteArrayProducerRecords = ProducerRecords[String, Array[Byte], Unit]
  type PublishToKafka[F[_]] =
    Pipe[F, ByteArrayProducerRecords, ByteArrayProducerResult]

  def consumer[F[_], A](
      topic: String,
      groupId: String,
      messageProcessingPipe: Pipe[F, A, A]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      brokers: KafkaBrokers
  ): F[List[A]] =
    ConsumerStream(topic, groupId).through(messageProcessingPipe).compile.toList

  def consumer[F[_], A](
      messageNum: Long,
      topic: String,
      groupId: String,
      messageProcessingPipe: Pipe[F, A, A]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      brokers: KafkaBrokers
  ): F[List[A]] =
    ConsumerStream(topic, groupId).through(messageProcessingPipe).take(messageNum).compile.toList

  def producer[F[_], A](
      topic: String,
      messageStream: Stream[F, Option[A]]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      encoder: Encoder[A],
      brokers: KafkaBrokers
  ): F[Unit] =
    messageStream
      .through(ProducerStream.pipe(brokers.urls, topic))
      .compile
      .drain

}
