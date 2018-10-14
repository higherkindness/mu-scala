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

package mu.rpc.client.cache

import cats.effect.{Effect, Timer}
import cats.instances.list._
import cats.instances.tuple._
import cats.syntax.apply._
import cats.syntax.bifunctor._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import fs2.{async, Stream}
import org.log4s.{getLogger, Logger}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration, MILLISECONDS}

trait ClientCache[Client[_[_]], F[_]] {

  def getClient: F[Client[F]]

}

object ClientCache {

  private val logger: Logger = getLogger

  type HostPort = (String, Int)

  def impl[Client[_[_]], F[_]](
      getHostAndPort: F[HostPort],
      createClient: HostPort => F[(Client[F], F[Unit])],
      tryToRemoveUnusedEvery: FiniteDuration,
      removeUnusedAfter: FiniteDuration
  )(
      implicit F: Effect[F],
      ec: ExecutionContext,
      timer: Timer[F]): Stream[F, ClientCache[Client, F]] = {

    type UnixMillis = Duration
    final case class ClientMeta(client: Client[F], close: F[Unit], lastAccessed: UnixMillis)
    type State = (Map[HostPort, ClientMeta], UnixMillis)

    val nowUnix: F[UnixMillis] =
      timer.clockRealTime(MILLISECONDS).map(_.millis)

    def create(ref: async.Ref[F, State]) = new ClientCache[Client, F] {
      val getClient: F[Client[F]] = for {
        hostAndPort <- getHostAndPort
        now         <- nowUnix
        (map, _)    <- ref.get
        client <- map
          .get(hostAndPort)
          .fold {
            createClient(hostAndPort).flatMap {
              case (client, close) =>
                F.delay(logger.info(s"Created new RPC client for $hostAndPort")) *>
                  ref
                    .modify(_.leftMap(_ + (hostAndPort -> ClientMeta(client, close, now))))
                    .as(client)
            }
          }(
            clientMeta =>
              F.delay(logger.debug(s"Reuse existing RPC client for $hostAndPort")) *>
                ref
                  .modify(_.leftMap(_.updated(hostAndPort, clientMeta.copy(lastAccessed = now))))
                  .as(clientMeta.client))
        (_, lastClean) <- ref.get
        _ <- if (lastClean < (now - tryToRemoveUnusedEvery))
          async.fork(cleanup(ref, _.lastAccessed < (now - removeUnusedAfter)))
        else F.unit
      } yield client
    }

    def cleanup(ref: async.Ref[F, State], canBeRemoved: ClientMeta => Boolean): F[Unit] =
      for {
        now <- nowUnix
        change <- ref.modify2 {
          case (map, _) =>
            val (remove, keep) = map.partition { case (_, clientMeta) => canBeRemoved(clientMeta) }
            ((keep, now), remove)
        }
        noLongerUsed = change._2.values.toList
        _ <- noLongerUsed.traverse_(_.close)
        _ <- F.delay(logger.info(s"Removed ${noLongerUsed.length} RPC clients from cache."))
      } yield ()

    val refState: F[async.Ref[F, State]] =
      nowUnix.tupleLeft(Map.empty[HostPort, ClientMeta]).flatMap(async.refOf[F, State])

    Stream.bracket(refState)(ref => Stream.emit(create(ref)), cleanup(_, _ => true))
  }

}
