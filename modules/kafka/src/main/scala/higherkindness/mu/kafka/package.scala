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

import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import fs2.{Pipe, Stream}
import fs2.kafka.{AutoOffsetReset, ProducerRecords, ProducerResult, ProducerSettings}
import higherkindness.mu.kafka.config.KafkaBrokers
import higherkindness.mu.format._

package object kafka {
  type ByteArrayProducerResult  = ProducerResult[String, Array[Byte], Unit]
  type ByteArrayProducerRecords = ProducerRecords[String, Array[Byte], Unit]
  type PublishToKafka[F[_]] =
    Pipe[F, ByteArrayProducerRecords, ByteArrayProducerResult]

  object consumerSettings {
    def atLeastOnceFromEarliest[F[_]: Sync](
        groupId: String,
        brokers: KafkaBrokers
    ): fs2.kafka.ConsumerSettings[F, String, Array[Byte]] =
      fs2.kafka
        .ConsumerSettings[F, String, Array[Byte]]
        .withGroupId(groupId)
        .withBootstrapServers(brokers.urls)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withEnableAutoCommit(true)
  }

  def consumer[F[_], A](
      topic: String,
      groupId: String,
      messageProcessingPipe: Pipe[F, A, A]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Deserialiser[A],
      brokers: KafkaBrokers
  ): F[Unit] =
    ConsumerStream(topic, consumerSettings.atLeastOnceFromEarliest(groupId, brokers))
      .through(messageProcessingPipe)
      .compile
      .drain

  object producerSettings {
    def apply[F[_]: Sync](brokers: KafkaBrokers): ProducerSettings[F, String, Array[Byte]] =
      ProducerSettings[F, String, Array[Byte]]
        .withBootstrapServers(brokers.urls)
  }
  def producer[F[_], A](
      topic: String,
      messageStream: Stream[F, Option[A]]
  )(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      encoder: Serialiser[A],
      brokers: KafkaBrokers
  ): F[Unit] =
    messageStream
      .through(
        ProducerStream.pipe(
          topic,
          producerSettings(brokers)
        )
      )
      .compile
      .drain

}
