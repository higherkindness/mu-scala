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

package higherkindness.mu.rpc
package common

import cats.data.Kleisli
import org.scalactic.Prettifier
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.compat.Platform
import scala.io._

trait RpcBaseTestSuite extends WordSpec with Matchers with OneInstancePerTest with MockFactory {

  trait Helpers {

    def runK[F[_], A, B](kleisli: Kleisli[F, A, B], v: A): F[B] =
      kleisli.run(v)

  }

  implicit val prettifier: Prettifier = Prettifier {
    case x => // includes null
      Platform.EOL + Prettifier.default(x) // initial linebreak makes expected/actual results line up nicely
  }

  // delegating the check to a def gets its name used in the cancellation message (cleaner than the boolean comparison result)
  private[this] def runningLocally: Boolean = System.getenv("TRAVIS") != "true"

  def ignoreOnTravis(msg: => String): Assertion = assume(runningLocally, msg)

  def resource(path: String): BufferedSource =
    Source.fromInputStream(getClass.getResourceAsStream(path))
}
