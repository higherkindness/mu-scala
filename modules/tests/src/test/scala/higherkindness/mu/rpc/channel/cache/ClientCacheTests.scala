/*
 * Copyright 2017-2020 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.channel.cache

import java.util.concurrent.TimeUnit

import cats.effect._
import cats.implicits._
import fs2.Stream
import higherkindness.mu.rpc.common.util.FakeClock

import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.{Ref, Temporal}

class ClientCacheTests extends AnyWordSpec with Matchers {

  val EC: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  private[this] val clockStep: Int = 50

  def buildTimer: IO[Temporal[IO]] =
    FakeClock.build[IO](clockStep.toLong, TimeUnit.MILLISECONDS).map { fakeClock =>
      new Temporal[IO] {

        private[this] val innerTimer = IO.timer(EC)

        override def clock: Clock[IO]                          = fakeClock
        override def sleep(duration: FiniteDuration): IO[Unit] = innerTimer.sleep(duration)
      }
    }

  implicit val cs: ContextShift[IO] = IO.contextShift(EC)

  def compiledStream[H](
      ref1: Ref[IO, Int],
      ref2: Ref[IO, Int],
      keys: List[H],
      cleanUp: Int
  ): IO[Unit] =
    (for {
      keyRef <- Stream.eval(Ref.of[IO, List[H]](keys))
      timer  <- Stream.eval(buildTimer)
      clientCache <- ClientCache.impl[MyClient, IO, H](
        keyRef.modify(list => (list.tail, list.head)),
        _ =>
          ref1
            .update(_ + 1)
            .map(_ => (new MyClient[IO], ref2.update(_ + 1).void)),
        cleanUp.millis,
        cleanUp.millis
      )(ConcurrentEffect[IO], cs, timer)
      _ <- Stream.eval((1 to keys.length).toList.traverse_(_ => clientCache.getClient))
    } yield ()).compile.drain

  def test(numClients: Int, cleanUp: Int): (Int, Int) =
    (for {
      ref1         <- Ref.of[IO, Int](0)
      ref2         <- Ref.of[IO, Int](0)
      _            <- compiledStream(ref1, ref2, (1 to numClients).toList.map("node-0000" + _), cleanUp)
      numCreations <- ref1.get
      numCloses    <- ref2.get
    } yield (numCreations, numCloses)).unsafeRunSync()

  "ClientCache.impl" should {

    "create the client and close after one use when the clean up time is lower than the elapsed time" in {
      test(3, clockStep - 10) shouldBe ((3, 3))
    }

    "create the client and close after one use when the clean up time is greater than the elapsed time" in {
      test(5, clockStep + 10) shouldBe ((5, 5))
    }

    "use the provided clients and close them" in {
      test(2, clockStep) shouldBe ((2, 2))
    }

  }

}

class MyClient[F[_]]
