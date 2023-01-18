/*
 * Copyright 2017-2023 47 Degrees Open Source <https://www.47deg.com>
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

package higherkindness.mu.rpc.protocol

import cats.effect.{unsafe, IO, Resource}
import munit.Suite

trait RPCFixtures { self: Suite =>

  def buildResourceFixture[A](name: String, resource: Resource[IO, A])(implicit
      runtime: unsafe.IORuntime
  ): Fixture[A] = new Fixture[A](name) {
    private var client: Option[A]          = None
    private var shutdown: Option[IO[Unit]] = None
    def apply() = client.getOrElse(throw new IllegalStateException("Not initialized"))
    override def beforeAll(): Unit = {
      val tuple = resource.allocated.unsafeRunSync()
      client = Some(tuple._1)
      shutdown = Some(tuple._2)
    }
    override def afterAll(): Unit =
      shutdown.foreach(_.unsafeRunSync())
  }

}
