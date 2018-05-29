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
package ssl

import java.io.File
import java.security.cert.X509Certificate

import cats.effect.Effect
import freestyle.rpc.common._
import freestyle.rpc.protocol._
import freestyle.rpc.server.netty.SetSslContext
import freestyle.rpc.server.{AddService, GrpcConfig, ServerW}
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.netty.handler.ssl.{ClientAuth, SslContext, SslProvider}

object Utils extends CommonUtils {

  object service {

    @service(Avro)
    trait RPCAvroService[F[_]] {
      @rpc def unary(a: A): F[C]
    }

    @service(AvroWithSchema)
    trait RPCAvroWithSchemaService[F[_]] {
      @rpc def unaryWithSchema(a: A): F[C]
    }

  }

  object handlers {

    object server {

      import database._
      import service._

      class ServerRPCService[F[_]: Effect]
          extends RPCAvroService[F]
          with RPCAvroWithSchemaService[F] {

        def unary(a: A): F[C] = Effect[F].delay(c1)

        def unaryWithSchema(a: A): F[C] = unary(a)

      }

    }

  }

  trait FreesRuntime {

    import service._
    import handlers.server._

    //////////////////////////////////
    // Server Runtime Configuration //
    //////////////////////////////////

    implicit val freesRPCHandler: ServerRPCService[ConcurrentMonad] =
      new ServerRPCService[ConcurrentMonad]

    val serverCertFile: File                         = TestUtils.loadCert("server1.pem")
    val serverPrivateKeyFile: File                   = TestUtils.loadCert("server1.key")
    val serverTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

    val serverSslContext: SslContext =
      GrpcSslContexts
        .configure(
          GrpcSslContexts.forServer(serverCertFile, serverPrivateKeyFile),
          SslProvider.OPENSSL)
        .trustManager(serverTrustedCaCerts: _*)
        .clientAuth(ClientAuth.REQUIRE)
        .build()

    val grpcConfigs: List[GrpcConfig] = List(
      SetSslContext(serverSslContext),
      AddService(RPCAvroService.bindService[ConcurrentMonad]),
      AddService(RPCAvroWithSchemaService.bindService[ConcurrentMonad])
    )

    implicit val serverW: ServerW = ServerW.netty(SC.port, grpcConfigs)

    //////////////////////////////////
    // Client Runtime Configuration //
    //////////////////////////////////

    // Create a client.
    val clientCertChainFile: File                    = TestUtils.loadCert("client.pem")
    val clientPrivateKeyFile: File                   = TestUtils.loadCert("client.key")
    val clientTrustedCaCerts: Array[X509Certificate] = Array(TestUtils.loadX509Cert("ca.pem"))

    val clientSslContext: SslContext =
      GrpcSslContexts.forClient
        .keyManager(clientCertChainFile, clientPrivateKeyFile)
        .trustManager(clientTrustedCaCerts: _*)
        .build()

  }

  object implicits extends FreesRuntime

}
