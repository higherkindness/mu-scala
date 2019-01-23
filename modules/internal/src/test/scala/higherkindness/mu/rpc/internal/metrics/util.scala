/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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

package higherkindness.mu.rpc.internal.metrics
import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync}

object util {

  object FakeClock {
    def apply[F[_]: Sync]: Clock[F] = new Clock[F] {
      private var count = 0L

      override def realTime(unit: TimeUnit): F[Long] = Sync[F].delay {
        count += 50
        unit.convert(count, TimeUnit.NANOSECONDS)
      }

      override def monotonic(unit: TimeUnit): F[Long] = Sync[F].delay {
        count += 50
        unit.convert(count, TimeUnit.NANOSECONDS)
      }
    }
  }

}
