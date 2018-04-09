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
package avro

import java.util.concurrent.TimeUnit
import java.util.UUID

import io.grpc.{ManagedChannel, Server, ServerServiceDefinition}
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.util.MutableHandlerRegistry
import freestyle.rpc.common._
import org.scalatest._

class RPCTests extends RpcBaseTestSuite {

  import freestyle.rpc.avro.Utils._
  import freestyle.rpc.avro.Utils.implicits._

  "frees-rpc client using avro serialization with schemas" should {

    def runTestProgram[T](ssd: ServerServiceDefinition): Assertion = {

      val serviceRegistry = new MutableHandlerRegistry

      val serverName = UUID.randomUUID.toString
      val serverBuilder: InProcessServerBuilder =
        InProcessServerBuilder
          .forName(serverName)
          .fallbackHandlerRegistry(serviceRegistry)
          .directExecutor()

      serverBuilder.addService(ssd)

      val server: Server = serverBuilder.build().start()

      val channelBuilder: InProcessChannelBuilder = InProcessChannelBuilder.forName(serverName)

      val channel: ManagedChannel = channelBuilder.directExecutor.build

      val rpcServiceClient: service.RPCService.Client[ConcurrentMonad] =
        service.RPCService.clientFromChannel[ConcurrentMonad](channel)

      val (r1, r2) = {
        (for {
          assertion1 <- rpcServiceClient.get(request)
          assertion2 <- rpcServiceClient.getCoproduct(requestCoproduct(request))
        } yield (assertion1, assertion2)).unsafeRunSync
      }

      channel.shutdown
      server.shutdown

      try {
        channel.awaitTermination(1, TimeUnit.MINUTES)
        server.awaitTermination(1, TimeUnit.MINUTES)
      } catch {
        case e: InterruptedException =>
          Thread.currentThread.interrupt()
          throw new RuntimeException(e)
      } finally {
        channel.shutdownNow
        server.shutdownNow
      }

      r1 shouldBe response
      r2 shouldBe responseCoproduct(response)
    }

    "be able to respond to a request" in {
      runTestProgram(rpcServiceDef)
    }

    "be able to respond to a request when the request model has added a boolean field" in {
      runTestProgram(rpcServiceRequestAddedBooleanDef)
    }

    "be able to respond to a request when the request model has added a string field" in {
      runTestProgram(rpcServiceRequestAddedStringDef)
    }

    "be able to respond to a request when the request model has added an int field" in {
      runTestProgram(rpcServiceRequestAddedIntDef)

    }

    "be able to respond to a request when the request model has added a field that is a case class" in {
      runTestProgram(rpcServiceRequestAddedNestedRequestDef)
    }

    "be able to respond to a request when the request model has dropped a field" in {
      runTestProgram(rpcServiceRequestDroppedFieldDef)
    }

    "be able to respond to a request when the response model has added a boolean field" in {
      runTestProgram(rpcServiceResponseAddedBooleanDef)
    }

    "be able to respond to a request when the response model has added a string field" in {
      runTestProgram(rpcServiceResponseAddedStringDef)

    }

    "be able to respond to a request when the response model has added an int field" in {
      runTestProgram(rpcServiceResponseAddedIntDef)
    }

    "be able to respond to a request when the response model has added a field that is a case class" in {
      runTestProgram(rpcServiceResponseAddedNestedResponseDef)
    }

    "be able to respond to a request when the response model has dropped a field" in {
      runTestProgram(rpcServiceResponseDroppedFieldDef)
    }
  }

}
