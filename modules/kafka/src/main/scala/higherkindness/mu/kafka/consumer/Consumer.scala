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

package higherkindness.mu.kafka.consumer

import cats.effect._
import fs2._
import fs2.kafka._
import higherkindness.mu.format.Decoder
import higherkindness.mu.kafka.config.KafkaBrokers
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.language.higherKinds

object Consumer {
  def consume[F[_], A](
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
    stream(topic, groupId).through(messageProcessingPipe).compile.toList

  def consumeN[F[_], A](
      messageNum: Int,
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
    stream(topic, groupId).through(messageProcessingPipe).take(messageNum).compile.toList

  def stream[F[_], A](topic: String, groupId: String)(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      brokers: KafkaBrokers
  ): Stream[F, A] =
    for {
      implicit0(logger: Logger[F]) <- Stream.eval(Slf4jLogger.create[F])
      s                            <- stream(fs2.kafka.consumerStream(Settings(groupId, brokers)))(topic)
    } yield s

  def stream[F[_], A](cs: Stream[F, KafkaConsumer[F, String, Array[Byte]]])(topic: String)(
      implicit contextShift: ContextShift[F],
      concurrentEffect: ConcurrentEffect[F],
      timer: Timer[F],
      decoder: Decoder[A],
      logger: Logger[F]
  ): Stream[F, A] =
    cs.evalTap(_.subscribeTo(topic))
      .flatMap(
        _.stream
          .flatMap { message =>
            val a = decoder.decode(message.record.value)
            for {
              _ <- Stream.eval(logger.info(a.toString))
            } yield a
          }
      )
}
