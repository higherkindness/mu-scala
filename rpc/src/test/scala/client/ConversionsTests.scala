/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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
package client

import cats.~>
import freestyle.async.implicits._
import freestyle.rpc.client.implicits._
import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ConversionsTests extends RpcClientTestSuite {

  import implicits._

  val handler: ListenableFuture ~> Future = listenableFuture2Async[Future]

  "Conversion methods" should {

    "transform guava ListenableFutures info scala.concurrent.Future successfully" in {

      handler(successfulFuture(foo)).await shouldEqual foo
    }

    "recover from failed guava ListenableFutures wrapping them into scala.concurrent.Future" in {

      val future: Future[String] = handler(failedFuture[String]) recover {
        case _ => foo
      }

      future.await shouldBe foo
    }

  }

}
