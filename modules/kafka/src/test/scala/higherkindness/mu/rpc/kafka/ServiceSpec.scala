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

package higherkindness.mu.rpc.kafka

import cats.effect.{ContextShift, IO, Resource, Sync}
import cats.syntax.applicative._
import fs2.kafka.AdminClientSettings
import higherkindness.mu.rpc.testing.servers.withServerChannel
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import io.grpc.{ManagedChannel, ServerServiceDefinition}
import org.apache.kafka.clients.admin.AdminClientConfig
import org.scalatest._

import scala.concurrent.ExecutionContext

import KafkaManagementService._

class ServiceSpec extends FunSuite with Matchers with OneInstancePerTest {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  def withClient[Client, A](
      serviceDef: IO[ServerServiceDefinition],
      resourceBuilder: IO[ManagedChannel] => Resource[IO, Client]
  )(
      f: Client => A
  ): A =
    withServerChannel(serviceDef)
      .flatMap(sc => resourceBuilder(IO(sc.channel)))
      .use(client => IO(f(client)))
      .unsafeRunSync()

  def adminClientSettings[F[_]: Sync](config: EmbeddedKafkaConfig): AdminClientSettings[F] =
    AdminClientSettings[F].withProperties(
      Map(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> s"localhost:${config.kafkaPort}")
    )

  def withKafka[F[_]: Sync, A](f: AdminClientSettings[F] => A): A =
    EmbeddedKafka.withRunningKafkaOnFoundPort(
      EmbeddedKafkaConfig()
    )(adminClientSettings[F] _ andThen f)

  test("create a topic") {
    withKafka[IO, IO[Unit]] { settings =>
      KafkaManagement.buildInstance[IO](settings).use { service =>
        implicit val s = service
        withClient(KafkaManagement.bindService[IO], KafkaManagement.clientFromChannel[IO](_)) {
          client =>
            for {
              create <- client.createTopic(CreateTopicRequest("topic", 2, 3)).attempt
              _      <- println(create).pure[IO]
              _      <- assert(create.isRight).pure[IO]
            } yield ()
        }
      }
    }.unsafeRunSync()
  }
}
