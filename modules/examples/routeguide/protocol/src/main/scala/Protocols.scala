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

package example.routeguide.protocol

import higherkindness.mu.rpc.protocol._
import monix.reactive.Observable

@outputPackage("routeguide")
@option("java_multiple_files", true)
@option("java_package", "io.grpc.examples.routeguide")
@option("java_outer_classname", "RouteGuideProto")
@option("objc_class_prefix", "RTG")
// Based on https://github.com/grpc/grpc-java/blob/v1.10.x/examples/src/main/proto/route_guide.proto
object Protocols {

  /**
   * Points are represented as latitude-longitude pairs in the E7 representation
   * (degrees multiplied by 10**7 and rounded to the nearest integer).
   * Latitudes should be in the range +/- 90 degrees and longitude should be in
   * the range +/- 180 degrees (inclusive).
   *
   * @param latitude Latitude.
   * @param longitude Longitude.
   */
  @message
  case class Point(latitude: Int, longitude: Int)

  /**
   * A latitude-longitude rectangle, represented as two diagonally opposite
   * points "lo" and "hi".
   *
   * @param lo One corner of the rectangle.
   * @param hi The other corner of the rectangle.
   */
  @message
  case class Rectangle(lo: Point, hi: Point)

  /**
   * A feature names something at a given point.
   * If a feature could not be named, the name is empty.
   *
   * @param name The name of the feature.
   * @param location The point where the feature is detected.
   */
  @message
  case class Feature(name: String, location: Point)

  /**
   * Not used in the RPC. Instead, this is here for the form serialized to disk.
   * @param feature Feature.
   */
  @message
  case class FeatureDatabase(feature: List[Feature])

  /**
   * A RouteNote is a message sent while at a given point.
   *
   * @param location The location from which the message is sent.
   * @param message The message to be sent.
   */
  @message
  case class RouteNote(location: Point, message: String)

  /**
   * A RouteSummary is received in response to a RecordRoute rpc.
   * It contains the number of individual points received, the number of
   * detected features, and the total distance covered as the cumulative sum of
   * the distance between each point.
   *
   * @param point_count The number of points received.
   * @param feature_count The number of known features passed while traversing the route.
   * @param distance The distance covered in metres.
   * @param elapsed_time The duration of the traversal in seconds.
   */
  @message
  case class RouteSummary(point_count: Int, feature_count: Int, distance: Int, elapsed_time: Int)

  @service(Protobuf)
  trait RouteGuideService[F[_]] {

    /**
     * A simple RPC.
     *
     * Obtains the feature at a given position.
     * A feature with an empty name is returned if there's no feature at the given position.
     *
     * @param point Position.
     * @return Feature at a given point.
     */
    def getFeature(point: Point): F[Feature]

    /**
     * A server-to-client streaming RPC.
     *
     * Obtains the Features available within the given Rectangle. Results are
     * streamed rather than returned at once (e.g. in a response message with a
     * repeated field), as the rectangle may cover a large area and contain a
     * huge number of features.
     *
     * @param rectangle Rectangle.
     * @return Features available within the given Rectangle, in a streaming fashion.
     */
    def listFeatures(rectangle: Rectangle): Observable[Feature]

    /**
     * A client-to-server streaming RPC.
     *
     * Accepts a stream of Points on a route being traversed, returning a
     * RouteSummary when traversal is completed.
     *
     * @param points Stream of points.
     * @return RouteSummary when traversal is completed.
     */
    def recordRoute(points: Observable[Point]): F[RouteSummary]

    /**
     * A Bidirectional streaming RPC.
     *
     * Accepts a stream of RouteNotes sent while a route is being traversed,
     * while receiving other RouteNotes (e.g. from other users).
     *
     * @param routeNotes Stream of RouteNotes.
     * @return Stream of RouteNotes.
     */
    def routeChat(routeNotes: Observable[RouteNote]): Observable[RouteNote]
  }

}
