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

package higherkindness.mu.http

import cats.effect.ConcurrentEffect
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.dsl.io._

sealed trait HttpMethod extends Product with Serializable
case object OPTIONS     extends HttpMethod
case object GET         extends HttpMethod
case object HEAD        extends HttpMethod
case object POST        extends HttpMethod
case object PUT         extends HttpMethod
case object DELETE      extends HttpMethod
case object TRACE       extends HttpMethod
case object CONNECT     extends HttpMethod
case object PATCH       extends HttpMethod

object HttpMethod {
  def fromString(str: String): Option[HttpMethod] = str match {
    case "OPTIONS" => Some(OPTIONS)
    case "GET"     => Some(GET)
    case "HEAD"    => Some(HEAD)
    case "POST"    => Some(POST)
    case "PUT"     => Some(PUT)
    case "DELETE"  => Some(DELETE)
    case "TRACE"   => Some(TRACE)
    case "CONNECT" => Some(CONNECT)
    case "PATCH"   => Some(PATCH)
    case _         => None
  }
}

case class RouteMap[F[_]](prefix: String, route: HttpRoutes[F])

object HttpServer {

  def bind[F[_]: ConcurrentEffect](
      port: Int,
      host: String,
      routes: RouteMap[F]*): BlazeServerBuilder[F] =
    BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(Router(routes.map(r => (s"/${r.prefix}", r.route)): _*).orNotFound)

}
