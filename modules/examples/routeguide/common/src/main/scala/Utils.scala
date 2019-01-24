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

package example.routeguide.common

import org.log4s._
import example.routeguide.protocol.Protocols._
import example.routeguide.common.Codecs._
import scala.io.Source
import scala.language.implicitConversions

object Utils {

  val logger = getLogger

  val features: List[Feature] = {
    logger.info("###### Loading route guide db...")
    io.circe.parser.decode[FeatureDatabase](
      Source
        .fromInputStream(getClass.getClassLoader.getResourceAsStream("route_guide_db.json"))
        .mkString) match {
      case Right(fList) => fList.feature
      case Left(e) =>
        logger.info(s"Decoding failure: $e")
        throw e
    }
  }

  implicit def pointOps(location: Point): PointOps      = new PointOps(location)
  implicit def featureOps(feature: Feature): FeatureOps = new FeatureOps(feature)

  final class PointOps(location: Point) {

    private val coordFactor = 1e7

    /**
     * Gets the latitude for the given point.
     */
    def getLatitude: Double = location.latitude / coordFactor

    /**
     * Gets the longitude for the given point.
     */
    def getLongitude: Double = location.longitude / coordFactor

    /**
     * Search a location among the list of specified features.
     *
     * @param features List of features where the location is searched.
     * @return The feature, an invalid feature is returned in case the
     *         location is not found.
     */
    def findFeatureIn(features: List[Feature]): Feature =
      features
        .find(f =>
          f.location.latitude == location.latitude && f.location.longitude == location.longitude)
        .getOrElse(Feature(name = "", location = location))

    def pretty: String = s"[$getLatitude, $getLongitude]"
  }

  final class FeatureOps(feature: Feature) {

    /**
     * Checks whether the given feature is valid (i.e. has a valid name).
     */
    def valid: Boolean = feature.name.nonEmpty

  }

  /**
   * Calculate the distance between two points using the "haversine" formula.
   * This code was taken from http://www.movable-type.co.uk/scripts/latlong.html.
   *
   * @param start The starting point
   * @param end   The end point
   * @return The distance between the points in meters
   */
  def calcDistance(start: Point, end: Point): Int = {
    val lat1: Double = start.getLatitude
    val lat2: Double = end.getLatitude
    val lon1: Double = start.getLongitude
    val lon2: Double = end.getLongitude
    val r: Int       = 6371000

    // meters
    val phi1: Double        = Math.toRadians(lat1)
    val phi2: Double        = Math.toRadians(lat2)
    val deltaPhi: Double    = Math.toRadians(lat2 - lat1)
    val deltaLambda: Double = Math.toRadians(lon2 - lon1)

    val a: Double = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
      Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
    val c: Double = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    (r * c).toInt
  }

}
