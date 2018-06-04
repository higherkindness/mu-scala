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

package example.routeguide.client.handlers

import cats._
import cats.implicits._
import example.routeguide.client.RouteGuideClient
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import monix.reactive.Observable
import org.log4s._
import example.routeguide.protocol.Protocols._
import example.routeguide.common.Utils._

import scala.concurrent.duration._

class RouteGuideClientHandler[F[_]: Monad](
    implicit client: RouteGuideService.Client[F],
    M: MonadError[F, Throwable],
    T2F: Task ~> F)
    extends RouteGuideClient[F] {

  val logger = getLogger

  override def getFeature(lat: Int, lon: Int): F[Unit] =
    M.handleErrorWith {
      logger.info(s"*** GetFeature: lat=$lat lon=$lon")
      client
        .getFeature(Point(lat, lon))
        .map { feature: Feature =>
          if (feature.valid)
            logger.info(s"Found feature called '${feature.name}' at ${feature.location.pretty}")
          else logger.info(s"Found no feature at ${feature.location.pretty}")
        }
    } {
      case e: StatusRuntimeException =>
        logger.warn(s"RPC failed:${e.getStatus} $e")
        M.raiseError(e)
    }

  override def listFeatures(lowLat: Int, lowLon: Int, hiLat: Int, hiLon: Int): F[Unit] = T2F.apply {
    logger.info(s"*** ListFeatures: lowLat=$lowLat lowLon=$lowLon hiLat=$hiLat hiLon=$hiLon")
    client
      .listFeatures(
        Rectangle(
          lo = Point(lowLat, lowLon),
          hi = Point(hiLat, hiLon)
        ))
      .zipWithIndex
      .map {
        case (feature, i) =>
          logger.info(s"Result #$i: $feature")
      }
      .onErrorHandle {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus} $e")
          throw e
      }
      .completedL
  }

  override def recordRoute(features: List[Feature], numPoints: Int): F[Unit] = {
    def takeN: List[Feature] = scala.util.Random.shuffle(features).take(numPoints)

    M.handleErrorWith {
      val points = takeN.map(_.location)
      logger.info(s"*** RecordRoute. Points: ${points.map(_.pretty).mkString(";")}")

      client
        .recordRoute(
          Observable
            .fromIterable(points)
            .delayOnNext(500.milliseconds))
        .map { summary: RouteSummary =>
          logger.info(
            s"Finished trip with ${summary.point_count} points. Passed ${summary.feature_count} features. " +
              s"Travelled ${summary.distance} meters. It took ${summary.elapsed_time} seconds.")
        }
    } { e: Throwable =>
      logger.warn(s"RecordRoute Failed: ${Status.fromThrowable(e)} $e")
      M.raiseError(e)
    }
  }

  override def routeChat: F[Unit] = T2F.apply {
    logger.info("*** RouteChat")
    client
      .routeChat(
        Observable
          .fromIterable(List(
            RouteNote(message = "First message", location = Point(0, 0)),
            RouteNote(message = "Second message", location = Point(0, 1)),
            RouteNote(message = "Third message", location = Point(1, 0)),
            RouteNote(message = "Fourth message", location = Point(1, 1))
          ))
          .delayOnNext(10.milliseconds)
          .map { routeNote =>
            logger.info(s"Sending message '${routeNote.message}' at " +
              s"${routeNote.location.latitude}, ${routeNote.location.longitude}")
            routeNote
          }
      )
      .map { note: RouteNote =>
        logger.info(s"Got message '${note.message}' at " +
          s"${note.location.latitude}, ${note.location.longitude}")
      }
      .onErrorHandle {
        case e: Throwable =>
          logger.warn(s"RouteChat Failed: ${Status.fromThrowable(e)} $e")
          throw e
      }
      .completedL
  }

}
