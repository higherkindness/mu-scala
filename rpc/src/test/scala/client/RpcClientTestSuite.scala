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

import java.util.concurrent.{Callable, Executors}

import com.google.common.util.concurrent.{
  ListenableFuture,
  ListeningExecutorService,
  MoreExecutors
}
import freestyle.rpc.client.utils.StringMarshaller
import io.grpc.{ClientCall, ManagedChannel, MethodDescriptor}

trait RpcClientTestSuite extends RpcBaseTestSuite {

  trait DummyData {

    type M = MethodDescriptor[String, String]
    type C = ClientCall[String, String]

    val managedChannelMock: ManagedChannel = mock[ManagedChannel]
    val clientCallMock: C                  = stub[C]

    val methodDescriptor: M = MethodDescriptor.create(
      MethodDescriptor.MethodType.UNARY,
      MethodDescriptor.generateFullMethodName("foo.Bar", "Bar"),
      new StringMarshaller(),
      new StringMarshaller()
    )

    val authority: String = "localhost:8696"
    val foo               = "Bar"

  }

  object implicits extends Helpers with DummyData {

    val service: ListeningExecutorService =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10))

    val exception: Throwable = new RuntimeException("Test exception")

    def failedFuture[T]: ListenableFuture[T] =
      service.submit(new Callable[T] {
        override def call(): T = throw exception
      })

    def successfulFuture[T](value: T): ListenableFuture[T] =
      service.submit(new Callable[T] {
        override def call(): T = value
      })
  }
}
