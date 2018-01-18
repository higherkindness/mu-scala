/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
package common

import cats.data.Kleisli
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

trait RpcBaseTestSuite extends WordSpec with Matchers with OneInstancePerTest with MockFactory {

  val sleepM: ConcurrentMonad[Unit] = delayM(Thread.sleep(500))

  trait Helpers {

    def runK[F[_], A, B](kleisli: Kleisli[F, A, B], v: A): F[B] =
      kleisli.run(v)

  }
}
