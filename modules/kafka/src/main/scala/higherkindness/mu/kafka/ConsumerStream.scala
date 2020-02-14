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

package higherkindness.mu.kafka

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import fs2.kafka.KafkaConsumer
import higherkindness.mu.format.Decoder
import higherkindness.mu.kafka.config.KafkaBrokers
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object ConsumerStream {

  /**
   * The API method for creating a consumer stream specialised with the fs2 Kafka consumer stream implementation
   * @param topic
   * @param groupId
   * @param contextShift
   * @param concurrentEffect
   * @param timer
   * @param decoder
   * @param brokers
   * @tparam F
   * @tparam A
   * @return
   */
  def apply[F[_], A](topic: String, groupId: String)(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      brokers: KafkaBrokers
  ): Stream[F, A] =
    for {
      implicit0(logger: Logger[F]) <- fs2.Stream.eval(Slf4jLogger.create[F])
      s                            <- apply(fs2.kafka.consumerStream(ConsumerSettings(groupId, brokers)))(topic)
    } yield s

  /**
   * A package private method for creating a consumer stream that has not been specialised with an implementation.
   * This enables unit testing of the stream logic without a running Kafka
   * @param kafkaConsumerStream
   * @param topic
   * @param contextShift
   * @param concurrentEffect
   * @param timer
   * @param decoder
   * @param logger
   * @tparam F
   * @tparam A
   * @return
   */
  private[kafka] def apply[F[_], A](
      kafkaConsumerStream: Stream[F, KafkaConsumer[F, String, Array[Byte]]]
  )(topic: String)(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      logger: Logger[F]
  ): Stream[F, A] =
    kafkaConsumerStream
      .evalTap(_.subscribeTo(topic))
      .flatMap(
        _.stream
          .flatMap { message =>
            val a = decoder.decode(message.record.value)
            for {
              _ <- fs2.Stream.eval(logger.info(a.toString))
            } yield a
          }
      )

}
