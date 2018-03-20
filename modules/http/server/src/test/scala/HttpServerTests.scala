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

package freestyle.rpc.http
package server

import cats.effect.IO
import monix.execution.Scheduler
import org.http4s.HttpService
import org.scalatest.{Assertion, Matchers, WordSpec}

class HttpServerTests extends WordSpec with Matchers {

  implicit val S: Scheduler  = monix.execution.Scheduler.Implicits.global
  implicit val C: HttpConfig = HttpConfig("0.0.0.0", 8090)

  val service: HttpService[IO] = new ExampleService[IO].service
  val prefix: String           = "/"

  def ok: Assertion = 1 shouldBe 1

  "HttpServerBuilder.build" should {

    "work as expected" in {

      new HttpServerBuilder[IO].build(service, prefix)

      ok
    }

  }
}
